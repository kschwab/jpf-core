package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
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

/** Fixture-specific verification of statics reconstructed directly from HPROF. */
public final class HprofStaticStateValidator implements HprofTestSupport {
  private static final String HOLDER = "HprofStaticStateGraph";
  private static final String PAYLOAD = "HprofStaticStateGraph$Payload";

  private int payloadRef;
  private int numbersRef;
  private int begunCycles;
  private int completedCycles;

  @Override
  public void initialize(
      VM vm, HprofView view, JpfHeapImporter.ImportResult result, int boundRootRef) {
    require(boundRootRef == MJIEnv.NULL,
        "G.1 fixture unexpectedly used the test root binder");
    ClassObj sourceClass = findExactlyOneClass(view, HOLDER);
    Map<Field, Object> source = sourceClass.getStaticFieldValues();
    requireStatic(source, "booleanValue", Type.BOOLEAN, Boolean.class, true);
    requireStatic(source, "byteValue", Type.BYTE, Byte.class, (byte) -7);
    requireStatic(source, "charValue", Type.CHAR, Character.class, '\u03A9');
    requireStatic(source, "shortValue", Type.SHORT, Short.class, (short) 30000);
    requireStatic(source, "intValue", Type.INT, Integer.class, 123456789);
    requireStatic(source, "longValue", Type.LONG, Long.class, 0x123456789ABCDEFL);
    requireStatic(source, "floatValue", Type.FLOAT, Float.class, 13.25f);
    requireStatic(source, "doubleValue", Type.DOUBLE, Double.class, -12345.125);

    ClassInstance payload = requireClassInstance(
        staticValue(source, "payload", Type.OBJECT), "payload");
    ClassInstance alias = requireClassInstance(
        staticValue(source, "payloadAlias", Type.OBJECT), "payloadAlias");
    require(payload.getId() == alias.getId(), "source payload statics do not alias");
    require(staticValue(source, "nullable", Type.OBJECT) == null,
        "source nullable static is not Java null");
    Object rawNumbers = staticValue(source, "numbers", Type.OBJECT);
    require(rawNumbers instanceof ArrayInstance, "source numbers is not an ArrayInstance");
    ArrayInstance numbers = (ArrayInstance) rawNumbers;
    require(numbers.getArrayType() == Type.INT, "source numbers is not Type.INT");

    Map<Long, Integer> refs = result.getRefMap();
    payloadRef = mapped(refs, payload.getId(), "Payload");
    numbersRef = mapped(refs, numbers.getId(), "numbers");
    require(refs.size() == 2 && new HashSet<>(refs.values()).size() == 2,
        "expected two distinct HPROF-to-JPF mappings");
    require(result.getAllocatedObjects() == 1 && result.getAllocatedArrays() == 1,
        "expected objects=1 and arrays=1");
    require(result.getPrimitiveFields() == 1 && result.getReferences() == 0,
        "unexpected instance field counts");
    require(result.getStaticPrimitiveFields() == 8,
        "expected staticPrimitiveFields=8");
    require(result.getStaticReferences() == 4, "expected staticReferences=4");
    require(result.getArrayElements() == 3, "expected arrayElements=3");

    verifyState(vm);
    System.out.println("[HPROF-JPF] static primitive values verified");
    System.out.println("[HPROF-JPF] static alias verified: payload=ref " + payloadRef
        + " payloadAlias=ref " + payloadRef);
    System.out.println("[HPROF-JPF] static null verified: nullable=" + MJIEnv.NULL);
    System.out.println("[HPROF-JPF] static array verified: numbers=ref " + numbersRef
        + " {4,5,6}");
    System.out.println("[HPROF-JPF] static fixture root binder absent: true");
  }

  @Override
  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] static-state GC begin: cycle=" + begunCycles);
  }

  @Override
  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end without matching GC begin");
    verifyState(vm);
    System.out.println("[HPROF-JPF] static-state GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] static-state controlled GC verified: cycles=1");
    }
  }

  private void verifyState(VM vm) {
    ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(HOLDER);
    require(ci.isInitialized(), "static holder is not initialized");
    require(ci.getClinit() == null, "static holder unexpectedly has <clinit>");
    StaticElementInfo statics = ci.getStaticElementInfo();
    require(statics != null, "static holder has no JPF static storage");
    require(statics.getBooleanField(field(ci, "booleanValue")), "booleanValue is not true");
    require(statics.getByteField(field(ci, "byteValue")) == -7, "byteValue mismatch");
    require(statics.getCharField(field(ci, "charValue")) == '\u03A9', "charValue mismatch");
    require(statics.getShortField(field(ci, "shortValue")) == 30000, "shortValue mismatch");
    require(statics.getIntField(field(ci, "intValue")) == 123456789, "intValue mismatch");
    require(statics.getLongField(field(ci, "longValue")) == 0x123456789ABCDEFL,
        "longValue mismatch");
    require(Float.floatToRawIntBits(statics.getFloatField(field(ci, "floatValue")))
            == Float.floatToRawIntBits(13.25f), "floatValue raw bits mismatch");
    require(Double.doubleToRawLongBits(statics.getDoubleField(field(ci, "doubleValue")))
            == Double.doubleToRawLongBits(-12345.125), "doubleValue raw bits mismatch");
    require(statics.getReferenceField(field(ci, "payload")) == payloadRef,
        "payload does not retain original JPF ref");
    require(statics.getReferenceField(field(ci, "payloadAlias")) == payloadRef,
        "payloadAlias does not alias payload");
    require(statics.getReferenceField(field(ci, "nullable")) == MJIEnv.NULL,
        "nullable is not JPF NULL");
    require(statics.getReferenceField(field(ci, "numbers")) == numbersRef,
        "numbers does not retain original JPF ref");

    ElementInfo payload = requireElement(vm, payloadRef, PAYLOAD, "Payload");
    require(payload.getIntField(field(
        ClassLoaderInfo.getSystemResolvedClassInfo(PAYLOAD), "id")) == 4242,
        "Payload.id is not 4242");
    ElementInfo numbers = requireElement(vm, numbersRef, "[I", "numbers");
    require(numbers.arrayLength() == 3 && numbers.getIntElement(0) == 4
        && numbers.getIntElement(1) == 5 && numbers.getIntElement(2) == 6,
        "numbers payload mismatch");
  }

  private static <T> void requireStatic(Map<Field, Object> values, String name, Type type,
      Class<T> wrapper, T expected) {
    Object value = staticValue(values, name, type);
    require(wrapper.isInstance(value), name + " has runtime type "
        + (value == null ? "null" : value.getClass().getName()));
    if (type == Type.FLOAT) {
      require(Float.floatToRawIntBits((Float) value) == Float.floatToRawIntBits((Float) expected),
          name + " source raw bits mismatch");
    } else if (type == Type.DOUBLE) {
      require(Double.doubleToRawLongBits((Double) value)
              == Double.doubleToRawLongBits((Double) expected),
          name + " source raw bits mismatch");
    } else {
      require(expected.equals(value), name + " source value mismatch");
    }
  }

  private static Object staticValue(Map<Field, Object> values, String name, Type type) {
    for (Map.Entry<Field, Object> entry : values.entrySet()) {
      if (name.equals(entry.getKey().getName())) {
        require(entry.getKey().getType() == type,
            name + " has unexpected HAHA Type " + entry.getKey().getType());
        return entry.getValue();
      }
    }
    throw new IllegalStateException("[HPROF-JPF] static-state validation failed: missing " + name);
  }

  private static ClassObj findExactlyOneClass(HprofView view, String className) {
    ClassObj found = null;
    for (ClassObj candidate : view.classes.values()) {
      if (className.equals(candidate.getClassName())) {
        require(found == null, "multiple HPROF ClassObj records for " + className);
        found = candidate;
      }
    }
    require(found != null, "no HPROF ClassObj for " + className);
    return found;
  }

  private static ClassInstance requireClassInstance(Object value, String description) {
    require(value instanceof ClassInstance, description + " is not a ClassInstance");
    return (ClassInstance) value;
  }

  private static FieldInfo field(ClassInfo ci, String fieldName) {
    FieldInfo field = ci.getDeclaredStaticField(fieldName);
    if (field == null) {
      field = ci.getDeclaredInstanceField(fieldName);
    }
    require(field != null, ci.getName() + "." + fieldName + " has no declared JPF FieldInfo");
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
      throw new IllegalStateException("[HPROF-JPF] static-state validation failed: " + message);
    }
  }
}
