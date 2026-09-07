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
    return importHeap(vm, view, selectedClassNames, Collections.emptySet());
  }

  public ImportResult importHeap(VM vm, HprofView view, Set<String> selectedClassNames,
      Set<String> selectedStaticClassNames) {
    if (selectedClassNames == null || selectedClassNames.isEmpty()) {
      throw new IllegalArgumentException("selected HPROF classes must not be empty");
    }
    if (selectedStaticClassNames == null) {
      throw new IllegalArgumentException("selected HPROF static classes must not be null");
    }
    ThreadInfo ti = vm.getCurrentThread();
    require(ti != null, "no current JPF thread during heap import");

    ImportState state = new ImportState(vm, view, selectedClassNames, selectedStaticClassNames, ti);
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
    private final Set<String> selectedStaticClassNames;
    private final ThreadInfo ti;
    private final List<ClassInstance> selectedInstances = new ArrayList<>();
    private final List<ClassObj> selectedStaticClasses = new ArrayList<>();
    private final Map<Long, ArrayInstance> selectedArrays = new LinkedHashMap<>();
    private final Map<Long, Integer> refMap = new LinkedHashMap<>();
    private int objects;
    private int arrays;
    private int primitiveFields;
    private int references;
    private int arrayElements;
    private int staticPrimitiveFields;
    private int staticReferences;

    ImportState(VM vm, HprofView view, Set<String> selectedClassNames,
        Set<String> selectedStaticClassNames, ThreadInfo ti) {
      this.jpfHeap = vm.getHeap();
      this.view = view;
      this.selectedClassNames = selectedClassNames;
      this.selectedStaticClassNames = selectedStaticClassNames;
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

      collectSelectedStaticClasses();
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
        if (array.getArrayType() == Type.OBJECT) {
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
          for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            setPrimitiveArrayElement(ei, i, array.getArrayType(), value);
            arrayElements++;
            System.out.printf("[HPROF-JPF] array-element HPROF=0x%x JPF=%d type=%s "
                    + "index=%d value=%s%n",
                array.getId(), jpfRef, array.getArrayType(), i, value);
          }
        }
      }

      populateStaticFields();
    }

    private void populateStaticFields() {
      for (ClassObj sourceClass : selectedStaticClasses) {
        String className = sourceClass.getClassName();
        ClassInfo ci = ClassLoaderInfo.getSystemResolvedClassInfo(className);
        require(ci.getClinit() == null,
            "HPROF static reconstruction for class " + className
                + " with <clinit> is not supported because initialization state is not reconstructed");
        if (!ci.isRegistered()) {
          ci.registerClass(ti);
        }
        boolean pushedClinit = ci.initializeClass(ti);
        require(!pushedClinit,
            "initializing static class " + className + " unexpectedly required <clinit>");
        require(ci.isInitialized(), "static class was not initialized: " + className);
        StaticElementInfo statics = ci.getModifiableStaticElementInfo();
        require(statics != null, "no modifiable JPF static storage for " + className);

        Map<Field, Object> sourceValues = sourceClass.getStaticFieldValues();
        List<Field> fields = new ArrayList<>(sourceValues.keySet());
        fields.sort(Comparator.comparing(Field::getName));
        for (Field sourceField : fields) {
          String fieldName = sourceField.getName();
          String description = "static field " + className + "." + fieldName;
          FieldInfo jpfField = ci.getDeclaredStaticField(fieldName);
          require(jpfField != null, description + " has no matching JPF FieldInfo");
          Object value = sourceValues.get(sourceField);
          if (sourceField.getType() == Type.OBJECT) {
            require(jpfField.isReference(), description + " is not a JPF reference field");
            int targetRef = MJIEnv.NULL;
            long targetId = 0;
            String targetClass = "null";
            if (value != null) {
              require(value instanceof Instance, description + " is not represented by an Instance");
              Instance target = (Instance) value;
              targetId = target.getId();
              targetRef = mappedRef(targetId, description + " target");
              targetClass = target instanceof ArrayInstance
                  ? jpfHeap.get(targetRef).getClassInfo().getName()
                  : target.getClassObj().getClassName();
            }
            statics.setReferenceField(jpfField, targetRef);
            staticReferences++;
            System.out.printf("[HPROF-JPF] static-reference class=%s field=%s "
                    + "targetHPROF=0x%x targetJPF=%d targetClass=%s%n",
                className, fieldName, targetId, targetRef, targetClass);
          } else {
            setPrimitiveField(statics, jpfField, sourceField.getType(), value, description);
            staticPrimitiveFields++;
            System.out.printf("[HPROF-JPF] static-field class=%s field=%s type=%s value=%s%n",
                className, fieldName, sourceField.getType(), value);
          }
        }
      }
    }

    private void setPrimitiveArrayElement(
        ElementInfo array, int index, Type type, Object value) {
      String description = type + " array element " + index;
      switch (type) {
        case BOOLEAN:
          require(value instanceof Boolean, description + " is not represented by a Boolean");
          array.setBooleanElement(index, (Boolean) value);
          return;
        case BYTE:
          require(value instanceof Byte, description + " is not represented by a Byte");
          array.setByteElement(index, (Byte) value);
          return;
        case CHAR:
          require(value instanceof Character, description + " is not represented by a Character");
          array.setCharElement(index, (Character) value);
          return;
        case SHORT:
          require(value instanceof Short, description + " is not represented by a Short");
          array.setShortElement(index, (Short) value);
          return;
        case INT:
          require(value instanceof Integer, description + " is not represented by an Integer");
          array.setIntElement(index, (Integer) value);
          return;
        case LONG:
          require(value instanceof Long, description + " is not represented by a Long");
          array.setLongElement(index, (Long) value);
          return;
        case FLOAT:
          require(value instanceof Float, description + " is not represented by a Float");
          array.setFloatElement(index, (Float) value);
          return;
        case DOUBLE:
          require(value instanceof Double, description + " is not represented by a Double");
          array.setDoubleElement(index, (Double) value);
          return;
        default:
          throw unsupported("primitive array payload has type " + type);
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

    private void collectSelectedStaticClasses() {
      Map<String, Integer> counts = new HashMap<>();
      for (ClassObj sourceClass : view.classes.values()) {
        String className = sourceClass.getClassName();
        if (selectedStaticClassNames.contains(className)) {
          selectedStaticClasses.add(sourceClass);
          counts.put(className, counts.getOrDefault(className, 0) + 1);
        }
      }
      selectedStaticClasses.sort(Comparator.comparing(ClassObj::getClassName));
      for (String className : selectedStaticClassNames) {
        require(counts.getOrDefault(className, 0) == 1,
            "expected exactly one HPROF ClassObj for selected static class " + className
                + ", found " + counts.getOrDefault(className, 0));
      }
    }

    private void collectDirectlyReferencedArrays() {
      for (ClassInstance instance : selectedInstances) {
        for (ClassInstance.FieldValue fieldValue : instance.getValues()) {
          if (fieldValue.getField().getType() == Type.OBJECT
              && fieldValue.getValue() instanceof ArrayInstance) {
            ArrayInstance array = (ArrayInstance) fieldValue.getValue();
            if (array.getArrayType() != Type.OBJECT) {
              jpfPrimitiveArraySignature(array.getArrayType());
            }
            selectedArrays.put(array.getId(), array);
          }
        }
      }
      for (ClassObj sourceClass : selectedStaticClasses) {
        for (Map.Entry<Field, Object> entry : sourceClass.getStaticFieldValues().entrySet()) {
          if (entry.getKey().getType() == Type.OBJECT
              && entry.getValue() instanceof ArrayInstance) {
            ArrayInstance array = (ArrayInstance) entry.getValue();
            if (array.getArrayType() != Type.OBJECT) {
              jpfPrimitiveArraySignature(array.getArrayType());
            }
            selectedArrays.put(array.getId(), array);
          }
        }
      }
    }

    private String jpfArrayElementType(ArrayInstance array) {
      if (array.getArrayType() == Type.OBJECT) {
        String arrayClassName = expectedJpfArrayClassName(array);
        require(arrayClassName.startsWith("[L") && arrayClassName.endsWith(";"),
            "unsupported object-array class name: " + arrayClassName);
        return arrayClassName.substring(1);
      }
      return jpfPrimitiveArraySignature(array.getArrayType());
    }

    private String expectedJpfArrayClassName(ArrayInstance array) {
      if (array.getArrayType() != Type.OBJECT) {
        return "[" + jpfPrimitiveArraySignature(array.getArrayType());
      }
      require(array.getClassObj() != null, "selected object array has no ClassObj");
      return array.getClassObj().getClassName();
    }

    private String jpfPrimitiveArraySignature(Type type) {
      switch (type) {
        case BOOLEAN: return "Z";
        case BYTE: return "B";
        case CHAR: return "C";
        case SHORT: return "S";
        case INT: return "I";
        case LONG: return "J";
        case FLOAT: return "F";
        case DOUBLE: return "D";
        default: throw unsupported("primitive array type " + type);
      }
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
      return new ImportResult(refMap, objects, arrays, primitiveFields, references, arrayElements,
          staticPrimitiveFields, staticReferences);
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
    private final int staticPrimitiveFields;
    private final int staticReferences;

    private ImportResult(Map<Long, Integer> refMap, int allocatedObjects, int allocatedArrays,
        int primitiveFields, int references, int arrayElements, int staticPrimitiveFields,
        int staticReferences) {
      this.refMap = Collections.unmodifiableMap(new LinkedHashMap<>(refMap));
      this.allocatedObjects = allocatedObjects;
      this.allocatedArrays = allocatedArrays;
      this.primitiveFields = primitiveFields;
      this.references = references;
      this.arrayElements = arrayElements;
      this.staticPrimitiveFields = staticPrimitiveFields;
      this.staticReferences = staticReferences;
    }

    public Map<Long, Integer> getRefMap() { return refMap; }
    public int getAllocatedObjects() { return allocatedObjects; }
    public int getAllocatedArrays() { return allocatedArrays; }
    public int getPrimitiveFields() { return primitiveFields; }
    public int getReferences() { return references; }
    public int getArrayElements() { return arrayElements; }
    public int getStaticPrimitiveFields() { return staticPrimitiveFields; }
    public int getStaticReferences() { return staticReferences; }
  }

  private static void printSummary(ImportResult result) {
    System.out.printf("[HPROF-JPF] import complete: objects=%d arrays=%d mappings=%d "
            + "primitiveFields=%d references=%d arrayElements=%d "
            + "staticPrimitiveFields=%d staticReferences=%d%n",
        result.getAllocatedObjects(), result.getAllocatedArrays(), result.getRefMap().size(),
        result.getPrimitiveFields(), result.getReferences(), result.getArrayElements(),
        result.getStaticPrimitiveFields(), result.getStaticReferences());
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
