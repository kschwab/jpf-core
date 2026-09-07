package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Type;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.VM;

import java.util.HashSet;
import java.util.Map;

/** Fixture-specific host verification for null, aliasing, cycles, and GC preservation. */
public final class HprofGraphIdentityValidator {
  private static final String HOLDER = "HprofGraphIdentityGraph";
  private static final String GRAPH = "HprofGraphIdentityGraph$Graph";
  private static final String NODE = "HprofGraphIdentityGraph$Node";

  private final int graphRef;
  private final int aRef;
  private final int bRef;
  private int begunCycles;
  private int completedCycles;

  private HprofGraphIdentityValidator(int graphRef, int aRef, int bRef) {
    this.graphRef = graphRef;
    this.aRef = aRef;
    this.bRef = bRef;
  }

  public static HprofGraphIdentityValidator create(
      VM vm, HprofView view, JpfHeapImporter.ImportResult result, int boundGraphRef) {
    ClassInstance graph = findExactlyOne(view, GRAPH);
    Object leftValue = fieldValue(graph, "left", Type.OBJECT);
    Object rightValue = fieldValue(graph, "right", Type.OBJECT);
    Object nullableValue = fieldValue(graph, "nullable", Type.OBJECT);
    require(leftValue instanceof ClassInstance, "Graph.left is not a ClassInstance");
    require(rightValue instanceof ClassInstance, "Graph.right is not a ClassInstance");
    require(nullableValue == null, "HAHA Graph.nullable is not represented as Java null");

    ClassInstance a = (ClassInstance) leftValue;
    require(((Instance) rightValue).getId() == a.getId(),
        "HPROF Graph.left and Graph.right do not alias Node(a)");
    Object bValue = fieldValue(a, "next", Type.OBJECT);
    require(bValue instanceof ClassInstance, "Node(a).next is not a ClassInstance");
    ClassInstance b = (ClassInstance) bValue;
    Object backValue = fieldValue(b, "next", Type.OBJECT);
    require(backValue instanceof ClassInstance && ((Instance) backValue).getId() == a.getId(),
        "HPROF Node(a) -> Node(b) -> Node(a) cycle is absent");
    require(integerField(a, "id") == 1, "HPROF Node(a).id is not 1");
    require(integerField(b, "id") == 2, "HPROF Node(b).id is not 2");
    require(integerField(graph, "marker") == 99, "HPROF Graph.marker is not 99");

    Map<Long, Integer> refs = result.getRefMap();
    int graphRef = mapped(refs, graph.getId(), "Graph");
    int aRef = mapped(refs, a.getId(), "Node(a)");
    int bRef = mapped(refs, b.getId(), "Node(b)");
    require(graphRef == boundGraphRef, "bound root is not the imported Graph reference");
    require(refs.size() == 3 && new HashSet<>(refs.values()).size() == 3,
        "expected three distinct HPROF-to-JPF mappings");
    require(result.getAllocatedObjects() == 3 && result.getAllocatedArrays() == 0,
        "expected objects=3 and arrays=0");
    require(result.getPrimitiveFields() == 3, "expected primitiveFields=3");
    require(result.getReferences() == 5,
        "expected references=5, including the explicit null assignment");

    System.out.println("[HPROF-JPF] HAHA null OBJECT representation: Graph.nullable raw=null");
    System.out.printf("[HPROF-JPF] graph mappings: Graph HPROF=0x%x JPF=%d "
            + "Node(a) HPROF=0x%x JPF=%d Node(b) HPROF=0x%x JPF=%d%n",
        graph.getId(), graphRef, a.getId(), aRef, b.getId(), bRef);

    HprofGraphIdentityValidator validator =
        new HprofGraphIdentityValidator(graphRef, aRef, bRef);
    validator.verifyGraph(vm);
    validator.printTopologyDiagnostics();
    return validator;
  }

  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] graph identity GC begin: cycle=" + begunCycles);
  }

  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end observed without matching GC begin");
    verifyGraph(vm);
    System.out.println("[HPROF-JPF] graph identity GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] graph identity controlled GC verified: cycles=1");
    }
  }

  private void verifyGraph(VM vm) {
    ClassInfo holder = ClassLoaderInfo.getSystemResolvedClassInfo(HOLDER);
    StaticElementInfo statics = holder.getStaticElementInfo();
    require(statics != null && statics.getReferenceField("root") == graphRef,
        "modeled static root does not contain the original Graph reference");

    ElementInfo graph = requireElement(vm, graphRef, GRAPH, "Graph");
    ElementInfo a = requireElement(vm, aRef, NODE, "Node(a)");
    ElementInfo b = requireElement(vm, bRef, NODE, "Node(b)");
    require(graph.getIntField("marker") == 99, "Graph.marker is not 99");
    require(graph.getReferenceField("left") == aRef, "Graph.left is not Node(a)");
    require(graph.getReferenceField("right") == aRef, "Graph.right does not alias Node(a)");
    require(graph.getReferenceField("nullable") == MJIEnv.NULL,
        "Graph.nullable is not JPF NULL");
    require(a.getIntField("id") == 1 && b.getIntField("id") == 2,
        "Node primitive IDs were not preserved");
    require(a.getReferenceField("next") == bRef, "Node(a).next is not Node(b)");
    require(b.getReferenceField("next") == aRef, "Node(b).next is not Node(a)");
  }

  private void printTopologyDiagnostics() {
    System.out.println("[HPROF-JPF] null verified: Graph.nullable=" + MJIEnv.NULL);
    System.out.println("[HPROF-JPF] alias verified: Graph.left=ref " + aRef
        + " Graph.right=ref " + aRef + " Node(a)=ref " + aRef);
    System.out.println("[HPROF-JPF] cycle verified: Node(a)=ref " + aRef
        + " -> Node(b)=ref " + bRef + " -> Node(a)=ref " + aRef);
  }

  private static ClassInstance findExactlyOne(HprofView view, String className) {
    ClassInstance found = null;
    for (Instance instance : view.instances.values()) {
      if (instance.getClassObj() != null && className.equals(instance.getClassObj().getClassName())) {
        require(instance instanceof ClassInstance, className + " is not a ClassInstance");
        require(found == null, "multiple instances found for " + className);
        found = (ClassInstance) instance;
      }
    }
    require(found != null, "no instance found for " + className);
    return found;
  }

  private static Object fieldValue(ClassInstance instance, String name, Type expectedType) {
    for (ClassInstance.FieldValue value : instance.getValues()) {
      if (name.equals(value.getField().getName())) {
        require(value.getField().getType() == expectedType,
            instance.getClassObj().getClassName() + "." + name + " has unexpected type");
        return value.getValue();
      }
    }
    throw new IllegalStateException("[HPROF-JPF] missing graph identity field " + name);
  }

  private static int integerField(ClassInstance instance, String name) {
    Object value = fieldValue(instance, name, Type.INT);
    require(value instanceof Integer, name + " is not represented by Integer");
    return (Integer) value;
  }

  private static int mapped(Map<Long, Integer> refs, long id, String description) {
    Integer ref = refs.get(id);
    require(ref != null && ref != MJIEnv.NULL, description + " has no non-null JPF mapping");
    return ref;
  }

  private static ElementInfo requireElement(VM vm, int ref, String className, String description) {
    ElementInfo element = vm.getHeap().get(ref);
    require(element != null, description + " was collected");
    require(className.equals(element.getClassInfo().getName()),
        description + " has unexpected class " + element.getClassInfo().getName());
    return element;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] graph identity validation failed: " + message);
    }
  }
}
