package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Type;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.VM;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Fixture-specific verification of inherited and hidden field layout. */
public final class HprofInheritanceValidator implements HprofTestSupport {
  private static final String HOLDER = "HprofInheritanceGraph";
  private static final String GRAPH = "HprofInheritanceGraph$InheritanceGraph";
  private static final String BASE = "HprofInheritanceGraph$Base";
  private static final String DERIVED = "HprofInheritanceGraph$Derived";
  private static final String HIDDEN_BASE = "HprofInheritanceGraph$HiddenBase";
  private static final String HIDDEN_DERIVED = "HprofInheritanceGraph$HiddenDerived";
  private static final String REF = "HprofInheritanceGraph$Ref";

  private int graphRef;
  private int derivedRef;
  private int hiddenRef;
  private int sharedRef;
  private int begunCycles;
  private int completedCycles;

  @Override
  public void initialize(
      VM vm, HprofView view, JpfHeapImporter.ImportResult result, int boundGraphRef) {
    ClassInstance graph = findExactlyOne(view, GRAPH);
    ClassInstance derived = requireClassInstance(
        fieldValue(graph, GRAPH, "derived", Type.OBJECT), "InheritanceGraph.derived");
    ClassInstance hidden = requireClassInstance(
        fieldValue(graph, GRAPH, "hidden", Type.OBJECT), "InheritanceGraph.hidden");
    ClassInstance baseShared = requireClassInstance(
        fieldValue(derived, BASE, "baseRef", Type.OBJECT), "Base.baseRef");
    ClassInstance derivedShared = requireClassInstance(
        fieldValue(derived, DERIVED, "derivedRef", Type.OBJECT), "Derived.derivedRef");

    require(baseShared.getId() == derivedShared.getId(),
        "Base.baseRef and Derived.derivedRef do not alias the same HPROF Ref");
    require(integerField(graph, GRAPH, "marker") == 99, "Graph.marker is not 99");
    require(integerField(derived, BASE, "baseValue") == 11, "Base.baseValue is not 11");
    require(integerField(derived, DERIVED, "derivedValue") == 22,
        "Derived.derivedValue is not 22");
    require(integerField(baseShared, REF, "id") == 7, "Ref.id is not 7");
    require(integerField(hidden, HIDDEN_BASE, "value") == 31,
        "HiddenBase.value is not 31");
    require(integerField(hidden, HIDDEN_DERIVED, "value") == 32,
        "HiddenDerived.value is not 32");

    Map<Long, Integer> refs = result.getRefMap();
    graphRef = mapped(refs, graph.getId(), "InheritanceGraph");
    derivedRef = mapped(refs, derived.getId(), "Derived");
    hiddenRef = mapped(refs, hidden.getId(), "HiddenDerived");
    sharedRef = mapped(refs, baseShared.getId(), "Ref");
    require(graphRef == boundGraphRef, "bound root is not the imported graph reference");
    require(refs.size() == 4 && new HashSet<>(refs.values()).size() == 4,
        "expected four distinct HPROF-to-JPF mappings");
    require(result.getAllocatedObjects() == 4 && result.getAllocatedArrays() == 0,
        "expected objects=4 and arrays=0");
    require(result.getPrimitiveFields() == 6, "expected primitiveFields=6");
    require(result.getReferences() == 4, "expected references=4");
    require(result.getArrayElements() == 0, "expected arrayElements=0");

    System.out.println("[HPROF-JPF] HAHA inherited field order: subclass declarations, "
        + "then each superclass declaration array");
    System.out.printf("[HPROF-JPF] inheritance mappings: Graph HPROF=0x%x JPF=%d "
            + "Derived HPROF=0x%x JPF=%d HiddenDerived HPROF=0x%x JPF=%d "
            + "Ref HPROF=0x%x JPF=%d%n",
        graph.getId(), graphRef, derived.getId(), derivedRef,
        hidden.getId(), hiddenRef, baseShared.getId(), sharedRef);

    verifyGraph(vm);
    System.out.println("[HPROF-JPF] inherited fields verified: Base.baseValue=11 "
        + "Derived.derivedValue=22");
    System.out.println("[HPROF-JPF] inherited reference alias verified: Base.baseRef=ref "
        + sharedRef + " Derived.derivedRef=ref " + sharedRef);
    System.out.println("[HPROF-JPF] hidden fields verified: HiddenBase.value=31 "
        + "HiddenDerived.value=32");
  }

  @Override
  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] inheritance GC begin: cycle=" + begunCycles);
  }

  @Override
  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end without matching GC begin");
    verifyGraph(vm);
    System.out.println("[HPROF-JPF] inheritance GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] inheritance controlled GC verified: cycles=1");
    }
  }

  private void verifyGraph(VM vm) {
    ClassInfo holder = ClassLoaderInfo.getSystemResolvedClassInfo(HOLDER);
    StaticElementInfo statics = holder.getStaticElementInfo();
    require(statics != null && statics.getReferenceField("root") == graphRef,
        "modeled root does not contain original graph reference");

    ElementInfo graph = requireElement(vm, graphRef, GRAPH, "InheritanceGraph");
    ElementInfo derived = requireElement(vm, derivedRef, DERIVED, "Derived");
    ElementInfo hidden = requireElement(vm, hiddenRef, HIDDEN_DERIVED, "HiddenDerived");
    ElementInfo shared = requireElement(vm, sharedRef, REF, "Ref");

    FieldInfo baseValue = declaredField(BASE, "baseValue");
    FieldInfo derivedValue = declaredField(DERIVED, "derivedValue");
    FieldInfo baseRef = declaredField(BASE, "baseRef");
    FieldInfo derivedRefField = declaredField(DERIVED, "derivedRef");
    FieldInfo hiddenBaseValue = declaredField(HIDDEN_BASE, "value");
    FieldInfo hiddenDerivedValue = declaredField(HIDDEN_DERIVED, "value");

    require(graph.getIntField(declaredField(GRAPH, "marker")) == 99, "Graph.marker is not 99");
    require(graph.getReferenceField(declaredField(GRAPH, "derived")) == derivedRef,
        "Graph.derived is not original Derived");
    require(graph.getReferenceField(declaredField(GRAPH, "hidden")) == hiddenRef,
        "Graph.hidden is not original HiddenDerived");
    require(derived.getIntField(baseValue) == 11, "JPF Base.baseValue is not 11");
    require(derived.getIntField(derivedValue) == 22, "JPF Derived.derivedValue is not 22");
    require(derived.getReferenceField(baseRef) == sharedRef,
        "JPF Base.baseRef is not original Ref");
    require(derived.getReferenceField(derivedRefField) == sharedRef,
        "JPF Derived.derivedRef does not alias Base.baseRef");
    require(shared.getIntField(declaredField(REF, "id")) == 7, "JPF Ref.id is not 7");
    require(hiddenBaseValue.getStorageOffset() != hiddenDerivedValue.getStorageOffset(),
        "hidden fields share a JPF storage offset");
    require(hidden.getIntField(hiddenBaseValue) == 31, "JPF HiddenBase.value is not 31");
    require(hidden.getIntField(hiddenDerivedValue) == 32, "JPF HiddenDerived.value is not 32");
  }

  private static FieldInfo declaredField(String className, String fieldName) {
    ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(className);
    FieldInfo field = ci.getDeclaredInstanceField(fieldName);
    require(field != null, className + "." + fieldName + " has no JPF FieldInfo");
    require(field.getClassInfo() == ci, className + "." + fieldName + " has wrong declaring ClassInfo");
    return field;
  }

  private static Object fieldValue(
      ClassInstance instance, String declaringClassName, String fieldName, Type type) {
    List<ClassInstance.FieldValue> values = instance.getValues();
    int index = 0;
    for (ClassObj declaring = instance.getClassObj();
        declaring != null; declaring = declaring.getSuperClassObj()) {
      for (Field field : declaring.getFields()) {
        require(index < values.size(), "HAHA flattened field list is too short");
        ClassInstance.FieldValue value = values.get(index++);
        require(value.getField() == field, "HAHA hierarchy/flattened field order mismatch");
        if (declaringClassName.equals(declaring.getClassName())
            && fieldName.equals(field.getName())) {
          require(field.getType() == type, declaringClassName + "." + fieldName + " has wrong type");
          return value.getValue();
        }
      }
    }
    throw new IllegalStateException("[HPROF-JPF] missing declared field "
        + declaringClassName + "." + fieldName);
  }

  private static int integerField(
      ClassInstance instance, String declaringClassName, String fieldName) {
    Object value = fieldValue(instance, declaringClassName, fieldName, Type.INT);
    require(value instanceof Integer, declaringClassName + "." + fieldName + " is not Integer");
    return (Integer) value;
  }

  private static ClassInstance requireClassInstance(Object value, String description) {
    require(value instanceof ClassInstance, description + " is not a ClassInstance");
    return (ClassInstance) value;
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

  private static int mapped(Map<Long, Integer> refs, long id, String description) {
    Integer ref = refs.get(id);
    require(ref != null && ref != 0, description + " has no non-null JPF mapping");
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
      throw new IllegalStateException("[HPROF-JPF] inheritance validation failed: " + message);
    }
  }
}
