package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Type;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.VM;

import java.util.HashSet;
import java.util.Map;

/** Fixture-specific verification of every primitive scalar field type. */
public final class HprofPrimitiveScalarValidator implements HprofTestSupport {
  private static final String HOLDER = "HprofPrimitiveScalarGraph";
  private static final String GRAPH = "HprofPrimitiveScalarGraph$PrimitiveGraph";
  private static final String VALUES = "HprofPrimitiveScalarGraph$PrimitiveValues";

  private int graphRef;
  private int valuesRef;
  private int begunCycles;
  private int completedCycles;

  @Override
  public void initialize(
      VM vm, HprofView view, JpfHeapImporter.ImportResult result, int boundGraphRef) {
    ClassInstance graph = findExactlyOne(view, GRAPH);
    ClassInstance values = requireClassInstance(
        sourceValue(graph, "values", Type.OBJECT, ClassInstance.class), "PrimitiveGraph.values");

    requireSource(graph, "marker", Type.INT, Integer.class, 55);
    requireSource(values, "booleanValue", Type.BOOLEAN, Boolean.class, true);
    requireSource(values, "byteValue", Type.BYTE, Byte.class, (byte) -7);
    requireSource(values, "charValue", Type.CHAR, Character.class, '\u03A9');
    requireSource(values, "shortValue", Type.SHORT, Short.class, (short) 30000);
    requireSource(values, "intValue", Type.INT, Integer.class, 123456789);
    requireSource(values, "longValue", Type.LONG, Long.class, 0x123456789ABCDEFL);
    requireSource(values, "floatValue", Type.FLOAT, Float.class, 13.25f);
    requireSource(values, "doubleValue", Type.DOUBLE, Double.class, -12345.125);

    Map<Long, Integer> refs = result.getRefMap();
    graphRef = mapped(refs, graph.getId(), "PrimitiveGraph");
    valuesRef = mapped(refs, values.getId(), "PrimitiveValues");
    require(graphRef == boundGraphRef, "bound root is not the imported graph reference");
    require(refs.size() == 2 && new HashSet<>(refs.values()).size() == 2,
        "expected two distinct HPROF-to-JPF mappings");
    require(result.getAllocatedObjects() == 2 && result.getAllocatedArrays() == 0,
        "expected objects=2 and arrays=0");
    require(result.getPrimitiveFields() == 9, "expected primitiveFields=9");
    require(result.getReferences() == 1, "expected references=1");
    require(result.getArrayElements() == 0, "expected arrayElements=0");

    verifyGraph(vm);
    printValues(vm);
  }

  @Override
  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] primitive-scalar GC begin: cycle=" + begunCycles);
  }

  @Override
  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end without matching GC begin");
    verifyGraph(vm);
    System.out.println("[HPROF-JPF] primitive-scalar GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] primitive-scalar controlled GC verified: cycles=1");
    }
  }

  private void verifyGraph(VM vm) {
    ClassInfo holder = ClassLoaderInfo.getSystemResolvedClassInfo(HOLDER);
    StaticElementInfo statics = holder.getStaticElementInfo();
    require(statics != null && statics.getReferenceField("root") == graphRef,
        "modeled root does not contain original PrimitiveGraph reference");

    ElementInfo graph = requireElement(vm, graphRef, GRAPH, "PrimitiveGraph");
    ElementInfo values = requireElement(vm, valuesRef, VALUES, "PrimitiveValues");
    require(graph.getIntField(declaredField(GRAPH, "marker")) == 55,
        "JPF marker is not 55");
    require(graph.getReferenceField(declaredField(GRAPH, "values")) == valuesRef,
        "JPF values field is not original PrimitiveValues");
    require(values.getBooleanField(declaredField(VALUES, "booleanValue")),
        "JPF booleanValue is not true");
    require(values.getByteField(declaredField(VALUES, "byteValue")) == (byte) -7,
        "JPF byteValue is not -7");
    require(values.getCharField(declaredField(VALUES, "charValue")) == '\u03A9',
        "JPF charValue is not U+03A9");
    require(values.getShortField(declaredField(VALUES, "shortValue")) == (short) 30000,
        "JPF shortValue is not 30000");
    require(values.getIntField(declaredField(VALUES, "intValue")) == 123456789,
        "JPF intValue mismatch");
    require(values.getLongField(declaredField(VALUES, "longValue")) == 0x123456789ABCDEFL,
        "JPF longValue mismatch");
    float restoredFloat = values.getFloatField(declaredField(VALUES, "floatValue"));
    double restoredDouble = values.getDoubleField(declaredField(VALUES, "doubleValue"));
    require(Float.floatToRawIntBits(restoredFloat) == Float.floatToRawIntBits(13.25f),
        "JPF floatValue raw bits mismatch");
    require(Double.doubleToRawLongBits(restoredDouble)
            == Double.doubleToRawLongBits(-12345.125),
        "JPF doubleValue raw bits mismatch");
  }

  private void printValues(VM vm) {
    ElementInfo values = requireElement(vm, valuesRef, VALUES, "PrimitiveValues");
    float floatValue = values.getFloatField(declaredField(VALUES, "floatValue"));
    double doubleValue = values.getDoubleField(declaredField(VALUES, "doubleValue"));
    System.out.println("[HPROF-JPF] primitive BOOLEAN booleanValue=true");
    System.out.println("[HPROF-JPF] primitive BYTE byteValue=-7");
    System.out.println("[HPROF-JPF] primitive CHAR charValue=U+03A9");
    System.out.println("[HPROF-JPF] primitive SHORT shortValue=30000");
    System.out.println("[HPROF-JPF] primitive INT intValue=123456789");
    System.out.println("[HPROF-JPF] primitive LONG longValue=0x0123456789ABCDEF");
    System.out.printf("[HPROF-JPF] primitive FLOAT floatValue=13.25 bits=0x%08X%n",
        Float.floatToRawIntBits(floatValue));
    System.out.printf("[HPROF-JPF] primitive DOUBLE doubleValue=-12345.125 bits=0x%016X%n",
        Double.doubleToRawLongBits(doubleValue));
  }

  private static <T> T requireSource(
      ClassInstance instance, String fieldName, Type type, Class<T> valueClass, T expected) {
    T value = sourceValue(instance, fieldName, type, valueClass);
    require(expected.equals(value), instance.getClassObj().getClassName() + "." + fieldName
        + " has unexpected HPROF value " + value);
    return value;
  }

  private static <T> T sourceValue(
      ClassInstance instance, String fieldName, Type type, Class<T> valueClass) {
    for (ClassInstance.FieldValue fieldValue : instance.getValues()) {
      Field field = fieldValue.getField();
      if (fieldName.equals(field.getName())) {
        require(field.getType() == type, fieldName + " has unexpected HAHA Type " + field.getType());
        Object value = fieldValue.getValue();
        require(valueClass.isInstance(value), fieldName + " has HAHA runtime value "
            + (value == null ? "null" : value.getClass().getName()));
        return valueClass.cast(value);
      }
    }
    throw new IllegalStateException("[HPROF-JPF] primitive-scalar validation failed: missing field "
        + instance.getClassObj().getClassName() + "." + fieldName);
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

  private static FieldInfo declaredField(String className, String fieldName) {
    ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(className);
    FieldInfo field = ci.getDeclaredInstanceField(fieldName);
    require(field != null, className + "." + fieldName + " has no JPF FieldInfo");
    require(field.getClassInfo() == ci, className + "." + fieldName + " has wrong declaring class");
    return field;
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
      throw new IllegalStateException("[HPROF-JPF] primitive-scalar validation failed: " + message);
    }
  }
}
