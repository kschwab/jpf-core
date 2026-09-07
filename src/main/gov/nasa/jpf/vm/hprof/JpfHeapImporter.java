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
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Two-pass importer for the deliberately narrow set of currently proven HPROF state. */
public final class JpfHeapImporter {
  public ImportResult importHeap(VM vm, HprofView view, Set<String> selectedClassNames) {
    if (selectedClassNames == null || selectedClassNames.isEmpty()) {
      throw new IllegalArgumentException("selected HPROF classes must not be empty");
    }
    ThreadInfo ti = vm.getCurrentThread();
    require(ti != null, "no current JPF thread during heap import");

    ImportState state = new ImportState(vm, view, selectedClassNames, ti);
    state.allocateObjectsAndArrays();       // Pass A: establish every identity.
    state.populateObjectsAndArrays();       // Pass B: translate state through that identity map.
    ImportResult result = state.toResult();
    printSummary(result);
    return result;
  }

  private static final class ImportState {
    private final gov.nasa.jpf.vm.Heap jpfHeap;
    private final HprofView view;
    private final Set<String> selectedClassNames;
    private final ThreadInfo ti;
    private final List<ClassInstance> selectedInstances = new ArrayList<>();
    private final Map<Long, ArrayInstance> selectedArrays = new LinkedHashMap<>();
    private final Map<Long, Integer> refMap = new LinkedHashMap<>();
    private int objects;
    private int arrays;
    private int primitiveFields;
    private int references;
    private int arrayElements;

    ImportState(VM vm, HprofView view, Set<String> selectedClassNames, ThreadInfo ti) {
      this.jpfHeap = vm.getHeap();
      this.view = view;
      this.selectedClassNames = selectedClassNames;
      this.ti = ti;
    }

    void allocateObjectsAndArrays() {
      collectSelectedInstances();
      for (ClassInstance instance : selectedInstances) {
        String className = instance.getClassObj().getClassName();
        ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(className);
        ElementInfo ei = jpfHeap.newObject(ci, ti);
        putIdentity(instance.getId(), ei.getObjectRef());
        objects++;
        System.out.printf("[HPROF-JPF] object HPROF=0x%x JPF=%d class=%s%n",
            instance.getId(), ei.getObjectRef(), className);
      }

      collectDirectlyReferencedArrays();
      for (ArrayInstance array : selectedArrays.values()) {
        String elementType = jpfArrayElementType(array);
        ElementInfo ei = jpfHeap.newArray(elementType, array.getValues().length, ti);
        putIdentity(array.getId(), ei.getObjectRef());
        arrays++;
        System.out.printf("[HPROF-JPF] array HPROF=0x%x JPF=%d type=%s length=%d%n",
            array.getId(), ei.getObjectRef(), ei.getClassInfo().getName(), array.getValues().length);
      }
    }

    void populateObjectsAndArrays() {
      for (ClassInstance instance : selectedInstances) {
        int jpfRef = mappedRef(instance.getId(), "selected object");
        ElementInfo ei = jpfHeap.getModifiable(jpfRef);
        for (DeclaredFieldValue fieldValue : declaredFieldValues(instance)) {
          String fieldName = fieldValue.field.getName();
          String declaringClassName = fieldValue.declaringClass.getClassName();
          Type fieldType = fieldValue.field.getType();
          Object value = fieldValue.value;
          ClassInfo declaringCi =
              ClassLoaderInfo.getSystemResolvedClassInfo(declaringClassName);
          FieldInfo jpfField = declaringCi.getDeclaredInstanceField(fieldName);
          require(jpfField != null,
              fieldDescription(declaringClassName, fieldName) + " has no matching JPF FieldInfo");
          if (fieldType == Type.OBJECT) {
            require(jpfField.isReference(),
                fieldDescription(declaringClassName, fieldName) + " is not a JPF reference field");
            int targetRef = MJIEnv.NULL;
            long targetId = 0;
            String targetClass = "null";
            if (value != null) {
              require(value instanceof Instance,
                  fieldDescription(declaringClassName, fieldName)
                      + " is not represented by an Instance");
              Instance target = (Instance) value;
              targetId = target.getId();
              targetRef = mappedRef(targetId,
                  fieldDescription(declaringClassName, fieldName) + " target");
              targetClass = target instanceof ArrayInstance
                  ? jpfHeap.get(targetRef).getClassInfo().getName()
                  : target.getClassObj().getClassName();
            }
            ei.setReferenceField(jpfField, targetRef);
            references++;
            System.out.printf("[HPROF-JPF] reference HPROF=0x%x JPF=%d class=%s field=%s "
                    + "targetHPROF=0x%x targetJPF=%d targetClass=%s%n",
                instance.getId(), jpfRef, declaringClassName, fieldName,
                targetId, targetRef, targetClass);
          } else {
            setPrimitiveField(ei, jpfField, fieldType, value,
                fieldDescription(declaringClassName, fieldName));
            primitiveFields++;
            System.out.printf("[HPROF-JPF] field HPROF=0x%x JPF=%d class=%s field=%s value=%s%n",
                instance.getId(), jpfRef, declaringClassName, fieldName, value);
          }
        }
      }

      for (ArrayInstance array : selectedArrays.values()) {
        int jpfRef = mappedRef(array.getId(), "selected array");
        ElementInfo ei = jpfHeap.getModifiable(jpfRef);
        Object[] values = array.getValues();
        String expectedClassName = expectedJpfArrayClassName(array);
        require(ei.isArray() && expectedClassName.equals(ei.getClassInfo().getName()),
            "mapped JPF object is not " + expectedClassName + " for HPROF 0x"
                + Long.toHexString(array.getId()));
        require(ei.arrayLength() == values.length,
            "HPROF and JPF array lengths differ for HPROF 0x" + Long.toHexString(array.getId()));
        if (array.getArrayType() == Type.INT) {
          for (int i = 0; i < values.length; i++) {
            require(values[i] instanceof Integer,
                "int[] element " + i + " is not represented by an Integer");
            int value = (Integer) values[i];
            ei.setIntElement(i, value);
            arrayElements++;
            System.out.printf("[HPROF-JPF] array-element HPROF=0x%x JPF=%d index=%d value=%d%n",
                array.getId(), jpfRef, i, value);
          }
        } else if (array.getArrayType() == Type.OBJECT) {
          for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            int targetRef = MJIEnv.NULL;
            long targetId = 0;
            String targetClass = "null";
            if (value != null) {
              require(value instanceof Instance,
                  "object-array element " + i + " is not represented by an Instance");
              Instance target = (Instance) value;
              targetId = target.getId();
              targetRef = mappedRef(targetId, "object-array element " + i + " target");
              targetClass = target.getClassObj().getClassName();
            }
            ei.setReferenceElement(i, targetRef);
            arrayElements++;
            System.out.printf("[HPROF-JPF] array-reference HPROF=0x%x JPF=%d index=%d "
                    + "targetHPROF=0x%x targetJPF=%d targetClass=%s%n",
                array.getId(), jpfRef, i, targetId, targetRef, targetClass);
          }
        } else {
          throw unsupported("selected array payload has type " + array.getArrayType());
        }
      }
    }

    private void setPrimitiveField(
        ElementInfo ei, FieldInfo field, Type type, Object value, String description) {
      switch (type) {
        case BOOLEAN:
          require(field.isBooleanField(), description + " is not a JPF boolean field");
          require(value instanceof Boolean, description + " did not contain a Boolean");
          ei.setBooleanField(field, (Boolean) value);
          return;
        case BYTE:
          require(field.isByteField(), description + " is not a JPF byte field");
          require(value instanceof Byte, description + " did not contain a Byte");
          ei.setByteField(field, (Byte) value);
          return;
        case CHAR:
          require(field.isCharField(), description + " is not a JPF char field");
          require(value instanceof Character, description + " did not contain a Character");
          ei.setCharField(field, (Character) value);
          return;
        case SHORT:
          require(field.isShortField(), description + " is not a JPF short field");
          require(value instanceof Short, description + " did not contain a Short");
          ei.setShortField(field, (Short) value);
          return;
        case INT:
          require(field.isIntField(), description + " is not a JPF int field");
          require(value instanceof Integer, description + " did not contain an Integer");
          ei.setIntField(field, (Integer) value);
          return;
        case LONG:
          require(field.isLongField(), description + " is not a JPF long field");
          require(value instanceof Long, description + " did not contain a Long");
          ei.setLongField(field, (Long) value);
          return;
        case FLOAT:
          require(field.isFloatField(), description + " is not a JPF float field");
          require(value instanceof Float, description + " did not contain a Float");
          ei.setFloatField(field, (Float) value);
          return;
        case DOUBLE:
          require(field.isDoubleField(), description + " is not a JPF double field");
          require(value instanceof Double, description + " did not contain a Double");
          ei.setDoubleField(field, (Double) value);
          return;
        default:
          throw unsupported(description + " has type " + type);
      }
    }

    private List<DeclaredFieldValue> declaredFieldValues(ClassInstance instance) {
      List<ClassInstance.FieldValue> flattened = instance.getValues();
      List<DeclaredFieldValue> declared = new ArrayList<>(flattened.size());
      int index = 0;
      for (ClassObj declaring = instance.getClassObj();
          declaring != null; declaring = declaring.getSuperClassObj()) {
        for (Field field : declaring.getFields()) {
          require(index < flattened.size(),
              "HAHA field hierarchy exceeds flattened values for " + instance.getClassObj().getClassName());
          ClassInstance.FieldValue value = flattened.get(index++);
          require(value.getField() == field,
              "HAHA flattened field order disagrees with ClassObj hierarchy at "
                  + fieldDescription(declaring.getClassName(), field.getName()));
          declared.add(new DeclaredFieldValue(declaring, field, value.getValue()));
        }
      }
      require(index == flattened.size(),
          "HAHA flattened values exceed field hierarchy for " + instance.getClassObj().getClassName());
      return declared;
    }

    private void collectSelectedInstances() {
      Map<String, Integer> counts = new HashMap<>();
      for (Instance instance : view.instances.values()) {
        ClassObj classObj = instance.getClassObj();
        if (classObj != null && selectedClassNames.contains(classObj.getClassName())) {
          require(instance instanceof ClassInstance,
              "selected ordinary object is not a ClassInstance: " + classObj.getClassName());
          selectedInstances.add((ClassInstance) instance);
          counts.put(classObj.getClassName(), counts.getOrDefault(classObj.getClassName(), 0) + 1);
        }
      }
      selectedInstances.sort(Comparator
          .comparing((ClassInstance i) -> i.getClassObj().getClassName())
          .thenComparingLong(Instance::getId));
      for (String className : selectedClassNames) {
        require(counts.getOrDefault(className, 0) > 0,
            "no HPROF instances found for selected class " + className);
      }
    }

    private void collectDirectlyReferencedArrays() {
      for (ClassInstance instance : selectedInstances) {
        for (ClassInstance.FieldValue fieldValue : instance.getValues()) {
          if (fieldValue.getField().getType() == Type.OBJECT
              && fieldValue.getValue() instanceof ArrayInstance) {
            ArrayInstance array = (ArrayInstance) fieldValue.getValue();
            require(array.getArrayType() == Type.INT || array.getArrayType() == Type.OBJECT,
                fieldDescription(instance, fieldValue.getField().getName())
                    + " references unsupported array type " + array.getArrayType());
            selectedArrays.put(array.getId(), array);
          }
        }
      }
    }

    private String jpfArrayElementType(ArrayInstance array) {
      if (array.getArrayType() == Type.INT) {
        return "I";
      }
      if (array.getArrayType() == Type.OBJECT) {
        String arrayClassName = expectedJpfArrayClassName(array);
        require(arrayClassName.startsWith("[L") && arrayClassName.endsWith(";"),
            "unsupported object-array class name: " + arrayClassName);
        return arrayClassName.substring(1);
      }
      throw unsupported("selected array type " + array.getArrayType());
    }

    private String expectedJpfArrayClassName(ArrayInstance array) {
      if (array.getArrayType() == Type.INT) {
        return "[I";
      }
      require(array.getClassObj() != null, "selected object array has no ClassObj");
      return array.getClassObj().getClassName();
    }

    private void putIdentity(long hprofId, int jpfRef) {
      require(jpfRef != MJIEnv.NULL, "allocation returned JPF NULL for HPROF 0x" + Long.toHexString(hprofId));
      require(!refMap.containsKey(hprofId), "duplicate allocation for HPROF 0x" + Long.toHexString(hprofId));
      require(!refMap.containsValue(jpfRef), "JPF reference reused by distinct HPROF IDs: " + jpfRef);
      refMap.put(hprofId, jpfRef);
    }

    private int mappedRef(long hprofId, String description) {
      Integer ref = refMap.get(hprofId);
      require(ref != null, description + " HPROF ID is outside the selected import graph: 0x"
          + Long.toHexString(hprofId));
      require(ref != MJIEnv.NULL, description + " maps to JPF NULL");
      return ref;
    }

    private ImportResult toResult() {
      return new ImportResult(refMap, objects, arrays, primitiveFields, references, arrayElements);
    }
  }

  private static final class DeclaredFieldValue {
    final ClassObj declaringClass;
    final Field field;
    final Object value;

    DeclaredFieldValue(ClassObj declaringClass, Field field, Object value) {
      this.declaringClass = declaringClass;
      this.field = field;
      this.value = value;
    }
  }

  public static final class ImportResult {
    private final Map<Long, Integer> refMap;
    private final int allocatedObjects;
    private final int allocatedArrays;
    private final int primitiveFields;
    private final int references;
    private final int arrayElements;

    private ImportResult(Map<Long, Integer> refMap, int allocatedObjects, int allocatedArrays,
        int primitiveFields, int references, int arrayElements) {
      this.refMap = Collections.unmodifiableMap(new LinkedHashMap<>(refMap));
      this.allocatedObjects = allocatedObjects;
      this.allocatedArrays = allocatedArrays;
      this.primitiveFields = primitiveFields;
      this.references = references;
      this.arrayElements = arrayElements;
    }

    public Map<Long, Integer> getRefMap() { return refMap; }
    public int getAllocatedObjects() { return allocatedObjects; }
    public int getAllocatedArrays() { return allocatedArrays; }
    public int getPrimitiveFields() { return primitiveFields; }
    public int getReferences() { return references; }
    public int getArrayElements() { return arrayElements; }
  }

  private static void printSummary(ImportResult result) {
    System.out.printf("[HPROF-JPF] import complete: objects=%d arrays=%d mappings=%d "
            + "primitiveFields=%d references=%d arrayElements=%d%n",
        result.getAllocatedObjects(), result.getAllocatedArrays(), result.getRefMap().size(),
        result.getPrimitiveFields(), result.getReferences(), result.getArrayElements());
  }

  private static String fieldDescription(ClassInstance instance, String fieldName) {
    return fieldDescription(instance.getClassObj().getClassName(), fieldName);
  }

  private static String fieldDescription(String declaringClassName, String fieldName) {
    return declaringClassName + "." + fieldName;
  }

  private static UnsupportedOperationException unsupported(String message) {
    return new UnsupportedOperationException("[HPROF-JPF] unsupported selected state: " + message);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException("[HPROF-JPF] " + message);
  }
}
