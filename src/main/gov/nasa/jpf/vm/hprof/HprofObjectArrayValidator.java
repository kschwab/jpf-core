package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ArrayInstance;
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

/** Fixture-specific verification of object-array identity, topology, and GC preservation. */
public final class HprofObjectArrayValidator implements HprofTestSupport {
  private static final String HOLDER = "HprofObjectArrayGraph";
  private static final String GRAPH = "HprofObjectArrayGraph$ArrayGraph";
  private static final String NODE = "HprofObjectArrayGraph$Node";
  private static final String NODE_ARRAY = "[LHprofObjectArrayGraph$Node;";

  private int graphRef;
  private int aRef;
  private int bRef;
  private int arrayRef;
  private int begunCycles;
  private int completedCycles;

  @Override
  public void initialize(
      VM vm, HprofView view, JpfHeapImporter.ImportResult result, int boundGraphRef) {
    ClassInstance graph = findExactlyOne(view, GRAPH);
    Object anchorValue = fieldValue(graph, "anchor", Type.OBJECT);
    Object nodesValue = fieldValue(graph, "nodes", Type.OBJECT);
    require(anchorValue instanceof ClassInstance, "ArrayGraph.anchor is not a ClassInstance");
    require(nodesValue instanceof ArrayInstance, "ArrayGraph.nodes is not an ArrayInstance");

    ClassInstance a = (ClassInstance) anchorValue;
    ArrayInstance nodes = (ArrayInstance) nodesValue;
    require(nodes.getArrayType() == Type.OBJECT, "Node[] getArrayType() is not OBJECT");
    Object[] values = nodes.getValues();
    require(values.getClass() == Object[].class, "Node[] values runtime type is not Object[]");
    require(values.length == 4, "Node[] length is not 4");
    require(values[0] instanceof ClassInstance, "nodes[0] is not a ClassInstance");
    require(values[1] instanceof ClassInstance, "nodes[1] is not a ClassInstance");
    require(values[2] instanceof ClassInstance, "nodes[2] is not a ClassInstance");
    require(values[3] == null, "HAHA null array element is not represented as Java null");
    require(((Instance) values[0]).getId() == a.getId(), "anchor and nodes[0] do not alias");
    require(((Instance) values[2]).getId() == a.getId(), "nodes[0] and nodes[2] do not alias");
    ClassInstance b = (ClassInstance) values[1];

    require(integerField(graph, "marker") == 77, "ArrayGraph.marker is not 77");
    require(integerField(a, "id") == 1, "Node(a).id is not 1");
    require(integerField(b, "id") == 2, "Node(b).id is not 2");
    require(instanceId(fieldValue(a, "peer", Type.OBJECT), "Node(a).peer") == b.getId(),
        "Node(a).peer is not Node(b)");
    require(instanceId(fieldValue(b, "peer", Type.OBJECT), "Node(b).peer") == a.getId(),
        "Node(b).peer is not Node(a)");
    require(instanceId(fieldValue(a, "links", Type.OBJECT), "Node(a).links") == nodes.getId(),
        "Node(a).links is not the shared Node[]");
    require(fieldValue(b, "links", Type.OBJECT) == null,
        "Node(b).links is not represented as Java null");

    Map<Long, Integer> refs = result.getRefMap();
    graphRef = mapped(refs, graph.getId(), "ArrayGraph");
    aRef = mapped(refs, a.getId(), "Node(a)");
    bRef = mapped(refs, b.getId(), "Node(b)");
    arrayRef = mapped(refs, nodes.getId(), "Node[]");
    require(graphRef == boundGraphRef, "bound root is not the imported ArrayGraph reference");
    require(refs.size() == 4 && new HashSet<>(refs.values()).size() == 4,
        "expected four distinct HPROF-to-JPF mappings");
    require(result.getAllocatedObjects() == 3 && result.getAllocatedArrays() == 1,
        "expected objects=3 and arrays=1");
    require(result.getPrimitiveFields() == 3, "expected primitiveFields=3");
    require(result.getReferences() == 6,
        "expected references=6, including the explicit null Node.links assignment");
    require(result.getArrayElements() == 4,
        "expected arrayElements=4, including the explicit null array element");

    System.out.println("[HPROF-JPF] HAHA object-array representation: type=OBJECT "
        + "valuesDeclared=Object[] valuesRuntime=" + values.getClass().getName()
        + " nonNullElement=" + values[0].getClass().getName() + " nullElement=null");
    System.out.printf("[HPROF-JPF] object-array mappings: ArrayGraph HPROF=0x%x JPF=%d "
            + "Node(a) HPROF=0x%x JPF=%d Node(b) HPROF=0x%x JPF=%d "
            + "Node[] HPROF=0x%x JPF=%d%n",
        graph.getId(), graphRef, a.getId(), aRef, b.getId(), bRef, nodes.getId(), arrayRef);

    verifyGraph(vm);
    System.out.println("[HPROF-JPF] object-array alias verified: graph.anchor=ref " + aRef
        + " nodes[0]=ref " + aRef + " nodes[2]=ref " + aRef);
    System.out.println("[HPROF-JPF] object-array null verified: nodes[3]=" + MJIEnv.NULL);
    System.out.println("[HPROF-JPF] object-array cycle verified: nodes=ref " + arrayRef
        + " -> Node(a)=ref " + aRef + " -> links=ref " + arrayRef);
  }

  @Override
  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] object-array GC begin: cycle=" + begunCycles);
  }

  @Override
  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end observed without matching GC begin");
    verifyGraph(vm);
    System.out.println("[HPROF-JPF] object-array GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] object-array controlled GC verified: cycles=1");
    }
  }

  private void verifyGraph(VM vm) {
    ClassInfo holder = ClassLoaderInfo.getSystemResolvedClassInfo(HOLDER);
    StaticElementInfo statics = holder.getStaticElementInfo();
    require(statics != null && statics.getReferenceField("root") == graphRef,
        "modeled static root does not contain the original ArrayGraph reference");

    ElementInfo graph = requireElement(vm, graphRef, GRAPH, "ArrayGraph");
    ElementInfo a = requireElement(vm, aRef, NODE, "Node(a)");
    ElementInfo b = requireElement(vm, bRef, NODE, "Node(b)");
    ElementInfo nodes = requireElement(vm, arrayRef, NODE_ARRAY, "Node[]");
    require(nodes.isArray() && nodes.arrayLength() == 4, "Node[] shell has wrong shape");
    require(graph.getIntField("marker") == 77, "ArrayGraph.marker is not 77");
    require(graph.getReferenceField("anchor") == aRef, "ArrayGraph.anchor is not Node(a)");
    require(graph.getReferenceField("nodes") == arrayRef, "ArrayGraph.nodes is not Node[]");
    require(a.getIntField("id") == 1 && b.getIntField("id") == 2,
        "Node primitive IDs were not preserved");
    require(a.getReferenceField("peer") == bRef && b.getReferenceField("peer") == aRef,
        "ordinary Node cycle was not preserved");
    require(a.getReferenceField("links") == arrayRef, "Node(a).links is not the shared Node[]");
    require(b.getReferenceField("links") == MJIEnv.NULL, "Node(b).links is not JPF NULL");
    require(nodes.getReferenceElement(0) == aRef, "nodes[0] is not Node(a)");
    require(nodes.getReferenceElement(1) == bRef, "nodes[1] is not Node(b)");
    require(nodes.getReferenceElement(2) == aRef, "nodes[2] does not alias Node(a)");
    require(nodes.getReferenceElement(3) == MJIEnv.NULL, "nodes[3] is not JPF NULL");
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
    throw new IllegalStateException("[HPROF-JPF] missing object-array field " + name);
  }

  private static int integerField(ClassInstance instance, String name) {
    Object value = fieldValue(instance, name, Type.INT);
    require(value instanceof Integer, name + " is not represented by Integer");
    return (Integer) value;
  }

  private static long instanceId(Object value, String description) {
    require(value instanceof Instance, description + " is not represented by Instance");
    return ((Instance) value).getId();
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
      throw new IllegalStateException("[HPROF-JPF] object-array validation failed: " + message);
    }
  }
}
