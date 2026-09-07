package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ArrayInstance;
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
import java.util.LinkedHashMap;
import java.util.Map;

/** Fixture-specific verification of every primitive array type. */
public final class HprofPrimitiveArrayValidator implements HprofTestSupport {
  private static final String HOLDER = "HprofPrimitiveArrayGraph";
  private static final String GRAPH = "HprofPrimitiveArrayGraph$PrimitiveArrayGraph";

  private int graphRef;
  private final Map<String, Integer> arrayRefs = new LinkedHashMap<>();
  private int begunCycles;
  private int completedCycles;

  @Override
  public void initialize(
      VM vm, HprofView view, JpfHeapImporter.ImportResult result, int boundGraphRef) {
    ClassInstance graph = findExactlyOne(view, GRAPH);
    requireSourceInt(graph, "marker", 66);

    Map<String, ArrayInstance> arrays = new LinkedHashMap<>();
    arrays.put("booleans", sourceArray(graph, "booleans", Type.BOOLEAN,
        Boolean.class, new Object[] {true, false, true}));
    arrays.put("bytes", sourceArray(graph, "bytes", Type.BYTE,
        Byte.class, new Object[] {(byte) -7, (byte) 0, (byte) 100}));
    arrays.put("chars", sourceArray(graph, "chars", Type.CHAR,
        Character.class, new Object[] {'A', '\u03A9', '\u0000'}));
    arrays.put("shorts", sourceArray(graph, "shorts", Type.SHORT,
        Short.class, new Object[] {(short) -1234, (short) 0, (short) 30000}));
    arrays.put("ints", sourceArray(graph, "ints", Type.INT,
        Integer.class, new Object[] {-123456789, 0, 123456789}));
    arrays.put("longs", sourceArray(graph, "longs", Type.LONG,
        Long.class, new Object[] {-0x123456789ABCDEFL, 0L, 0x123456789ABCDEFL}));
    arrays.put("floats", sourceArray(graph, "floats", Type.FLOAT,
        Float.class, new Object[] {-13.25f, 0.0f, 13.25f}));
    arrays.put("doubles", sourceArray(graph, "doubles", Type.DOUBLE,
        Double.class, new Object[] {-12345.125, 0.0, 12345.125}));

    Map<Long, Integer> refs = result.getRefMap();
    graphRef = mapped(refs, graph.getId(), "PrimitiveArrayGraph");
    require(graphRef == boundGraphRef, "bound root is not the imported graph reference");
    for (Map.Entry<String, ArrayInstance> entry : arrays.entrySet()) {
      arrayRefs.put(entry.getKey(), mapped(refs, entry.getValue().getId(), entry.getKey()));
    }
    require(refs.size() == 9 && new HashSet<>(refs.values()).size() == 9,
        "expected nine distinct HPROF-to-JPF mappings");
    require(result.getAllocatedObjects() == 1 && result.getAllocatedArrays() == 8,
        "expected objects=1 and arrays=8");
    require(result.getPrimitiveFields() == 1, "expected primitiveFields=1");
    require(result.getReferences() == 8, "expected references=8");
    require(result.getArrayElements() == 24, "expected arrayElements=24");

    verifyGraph(vm);
    printValues(vm);
  }

  @Override
  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] primitive-array GC begin: cycle=" + begunCycles);
  }

  @Override
  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end without matching GC begin");
    verifyGraph(vm);
    System.out.println("[HPROF-JPF] primitive-array GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] primitive-array controlled GC verified: cycles=1");
    }
  }

  private void verifyGraph(VM vm) {
    StaticElementInfo statics = ClassLoaderInfo.getSystemResolvedClassInfo(HOLDER)
        .getStaticElementInfo();
    require(statics != null && statics.getReferenceField("root") == graphRef,
        "modeled root does not contain original PrimitiveArrayGraph reference");
    ElementInfo graph = requireElement(vm, graphRef, GRAPH, "PrimitiveArrayGraph");
    require(graph.getIntField(declaredField("marker")) == 66, "JPF marker is not 66");

    ElementInfo booleans = array(vm, graph, "booleans", "[Z");
    require(booleans.getBooleanElement(0) && !booleans.getBooleanElement(1)
        && booleans.getBooleanElement(2), "JPF boolean[] mismatch");
    ElementInfo bytes = array(vm, graph, "bytes", "[B");
    require(bytes.getByteElement(0) == -7 && bytes.getByteElement(1) == 0
        && bytes.getByteElement(2) == 100, "JPF byte[] mismatch");
    ElementInfo chars = array(vm, graph, "chars", "[C");
    require(chars.getCharElement(0) == 'A' && chars.getCharElement(1) == '\u03A9'
        && chars.getCharElement(2) == '\u0000', "JPF char[] mismatch");
    ElementInfo shorts = array(vm, graph, "shorts", "[S");
    require(shorts.getShortElement(0) == -1234 && shorts.getShortElement(1) == 0
        && shorts.getShortElement(2) == 30000, "JPF short[] mismatch");
    ElementInfo ints = array(vm, graph, "ints", "[I");
    require(ints.getIntElement(0) == -123456789 && ints.getIntElement(1) == 0
        && ints.getIntElement(2) == 123456789, "JPF int[] mismatch");
    ElementInfo longs = array(vm, graph, "longs", "[J");
    require(longs.getLongElement(0) == -0x123456789ABCDEFL && longs.getLongElement(1) == 0L
        && longs.getLongElement(2) == 0x123456789ABCDEFL, "JPF long[] mismatch");
    ElementInfo floats = array(vm, graph, "floats", "[F");
    requireFloatBits(floats, 0, -13.25f);
    requireFloatBits(floats, 1, 0.0f);
    requireFloatBits(floats, 2, 13.25f);
    ElementInfo doubles = array(vm, graph, "doubles", "[D");
    requireDoubleBits(doubles, 0, -12345.125);
    requireDoubleBits(doubles, 1, 0.0);
    requireDoubleBits(doubles, 2, 12345.125);
  }

  private ElementInfo array(VM vm, ElementInfo graph, String fieldName, String className) {
    int expectedRef = arrayRefs.get(fieldName);
    require(graph.getReferenceField(declaredField(fieldName)) == expectedRef,
        fieldName + " does not retain its original imported array reference");
    ElementInfo array = requireElement(vm, expectedRef, className, fieldName);
    require(array.isArray() && array.arrayLength() == 3, fieldName + " is not a length-3 array");
    return array;
  }

  private void printValues(VM vm) {
    ElementInfo graph = requireElement(vm, graphRef, GRAPH, "PrimitiveArrayGraph");
    ElementInfo floats = array(vm, graph, "floats", "[F");
    ElementInfo doubles = array(vm, graph, "doubles", "[D");
    System.out.println("[HPROF-JPF] array BOOLEAN booleans={true,false,true}");
    System.out.println("[HPROF-JPF] array BYTE bytes={-7,0,100}");
    System.out.println("[HPROF-JPF] array CHAR chars={U+0041,U+03A9,U+0000}");
    System.out.println("[HPROF-JPF] array SHORT shorts={-1234,0,30000}");
    System.out.println("[HPROF-JPF] array INT ints={-123456789,0,123456789}");
    System.out.println("[HPROF-JPF] array LONG longs verified");
    for (int i = 0; i < 3; i++) {
      System.out.printf("[HPROF-JPF] array FLOAT index=%d value=%s bits=0x%08X%n",
          i, floats.getFloatElement(i), Float.floatToRawIntBits(floats.getFloatElement(i)));
      System.out.printf("[HPROF-JPF] array DOUBLE index=%d value=%s bits=0x%016X%n",
          i, doubles.getDoubleElement(i), Double.doubleToRawLongBits(doubles.getDoubleElement(i)));
    }
  }

  private static ArrayInstance sourceArray(ClassInstance graph, String fieldName, Type type,
      Class<?> wrapper, Object[] expected) {
    Object raw = sourceField(graph, fieldName, Type.OBJECT);
    require(raw instanceof ArrayInstance, fieldName + " is not an ArrayInstance");
    ArrayInstance array = (ArrayInstance) raw;
    require(array.getArrayType() == type, fieldName + " has HAHA type " + array.getArrayType());
    Object[] values = array.getValues();
    require(values.getClass() == Object[].class && values.length == expected.length,
        fieldName + " does not have runtime Object[] length " + expected.length);
    for (int i = 0; i < values.length; i++) {
      require(wrapper.isInstance(values[i]), fieldName + " element " + i
          + " has runtime type " + (values[i] == null ? "null" : values[i].getClass().getName()));
      if (type == Type.FLOAT) {
        require(Float.floatToRawIntBits((Float) values[i])
                == Float.floatToRawIntBits((Float) expected[i]),
            fieldName + " source float bits differ at " + i);
      } else if (type == Type.DOUBLE) {
        require(Double.doubleToRawLongBits((Double) values[i])
                == Double.doubleToRawLongBits((Double) expected[i]),
            fieldName + " source double bits differ at " + i);
      } else {
        require(expected[i].equals(values[i]), fieldName + " source value differs at " + i);
      }
    }
    return array;
  }

  private static void requireSourceInt(ClassInstance graph, String fieldName, int expected) {
    Object value = sourceField(graph, fieldName, Type.INT);
    require(value instanceof Integer && (Integer) value == expected,
        fieldName + " is not Integer " + expected);
  }

  private static Object sourceField(ClassInstance instance, String fieldName, Type type) {
    for (ClassInstance.FieldValue value : instance.getValues()) {
      Field field = value.getField();
      if (fieldName.equals(field.getName())) {
        require(field.getType() == type, fieldName + " has unexpected field Type " + field.getType());
        return value.getValue();
      }
    }
    throw new IllegalStateException("[HPROF-JPF] primitive-array validation failed: missing field "
        + fieldName);
  }

  private static void requireFloatBits(ElementInfo array, int index, float expected) {
    require(Float.floatToRawIntBits(array.getFloatElement(index)) == Float.floatToRawIntBits(expected),
        "JPF float[] raw bits mismatch at " + index);
  }

  private static void requireDoubleBits(ElementInfo array, int index, double expected) {
    require(Double.doubleToRawLongBits(array.getDoubleElement(index))
            == Double.doubleToRawLongBits(expected),
        "JPF double[] raw bits mismatch at " + index);
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

  private static FieldInfo declaredField(String fieldName) {
    ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(GRAPH);
    FieldInfo field = ci.getDeclaredInstanceField(fieldName);
    require(field != null, GRAPH + "." + fieldName + " has no JPF FieldInfo");
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
      throw new IllegalStateException("[HPROF-JPF] primitive-array validation failed: " + message);
    }
  }
}
