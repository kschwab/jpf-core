package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Type;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.VM;

import java.util.HashSet;
import java.util.Map;

/** Smoke-specific expectations kept separate from reconstruction semantics. */
public final class HprofPassASmokeValidator {
  private static final String FOO = "HprofPassASmokeGraph$Foo";
  private static final String BAR = "HprofPassASmokeGraph$Bar";

  private HprofPassASmokeValidator() {}

  public static void validate(VM vm, HprofView view, JpfHeapImporter.ImportResult result) {
    ClassInstance foo = findExactlyOne(view, FOO);
    ClassInstance bar = findExactlyOne(view, BAR);
    Object numbersValue = fieldValue(foo, "numbers", Type.OBJECT);
    require(numbersValue instanceof ArrayInstance, "Foo.numbers is not an ArrayInstance");
    ArrayInstance numbers = (ArrayInstance) numbersValue;

    Map<Long, Integer> refMap = result.getRefMap();
    int fooRef = mapped(refMap, foo.getId(), "Foo");
    int barRef = mapped(refMap, bar.getId(), "Bar");
    int arrayRef = mapped(refMap, numbers.getId(), "Foo.numbers int[]");
    require(refMap.size() == 3, "expected exactly three identity mappings");
    require(new HashSet<>(refMap.values()).size() == 3, "JPF references are not distinct");

    ElementInfo fooEi = vm.getHeap().get(fooRef);
    ElementInfo barEi = vm.getHeap().get(barRef);
    ElementInfo arrayEi = vm.getHeap().get(arrayRef);
    require(fooEi != null && FOO.equals(fooEi.getClassInfo().getName()), "wrong Foo shell");
    require(barEi != null && BAR.equals(barEi.getClassInfo().getName()), "wrong Bar shell");
    require(arrayEi != null && arrayEi.isArray() && "[I".equals(arrayEi.getClassInfo().getName()),
        "wrong int[] shell");
    require(arrayEi.arrayLength() == 3, "int[] length is not 3");
    System.out.println("[HPROF-JPF] Pass A verified: objects=2 arrays=1 mappings=3");

    require(fooEi.getIntField("value") == 123, "Foo.value is not 123");
    require(barEi.getIntField("number") == 42, "Bar.number is not 42");
    System.out.println("[HPROF-JPF] Pass B.1 verified: primitiveFields=2");
    System.out.println("[HPROF-JPF] Foo.value=123");
    System.out.println("[HPROF-JPF] Bar.number=42");

    Object childValue = fieldValue(foo, "child", Type.OBJECT);
    require(childValue instanceof ClassInstance, "Foo.child is not a ClassInstance");
    require(((Instance) childValue).getId() == bar.getId(), "HPROF Foo.child does not target Bar");
    require(fooEi.getReferenceField("child") == barRef,
        "JPF Foo.child does not use the Pass A Bar reference");
    require(vm.getHeap().get(fooEi.getReferenceField("child")).getIntField("number") == 42,
        "Bar.number did not survive through Foo.child");
    System.out.println("[HPROF-JPF] Pass B.2 verified: references=1");
    System.out.println("[HPROF-JPF] Foo.child -> JPF ref " + barRef + " (" + BAR + ")");

    require(numbers.getArrayType() == Type.INT, "HPROF Foo.numbers is not int[]");
    require(fooEi.getReferenceField("numbers") == arrayRef,
        "JPF Foo.numbers does not use the Pass A array reference");
    System.out.println("[HPROF-JPF] Pass B.3 verified: arrayReferences=1");
    System.out.println("[HPROF-JPF] Foo.numbers -> JPF ref " + arrayRef + " ([I length=3)");

    require(arrayEi.getIntElement(0) == 10 && arrayEi.getIntElement(1) == 20
        && arrayEi.getIntElement(2) == 30, "JPF int[] does not contain {10,20,30}");
    System.out.println("[HPROF-JPF] Pass B.4 verified: arrayElements=3");
    System.out.println("[HPROF-JPF] Foo.numbers={10,20,30}");
    System.out.printf("[HPROF-JPF] smoke graph verified: Foo(ref %d){value=123, child=%d, numbers=%d} "
        + "Bar(ref %d){number=42} int[](ref %d){10,20,30}%n",
        fooRef, barRef, arrayRef, barRef, arrayRef);
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

  private static Object fieldValue(ClassInstance instance, String name, Type type) {
    for (ClassInstance.FieldValue fieldValue : instance.getValues()) {
      if (name.equals(fieldValue.getField().getName())) {
        require(fieldValue.getField().getType() == type,
            instance.getClassObj().getClassName() + "." + name + " has unexpected type");
        return fieldValue.getValue();
      }
    }
    throw new IllegalStateException("[HPROF-JPF] missing smoke field " + name);
  }

  private static int mapped(Map<Long, Integer> refMap, long id, String description) {
    Integer ref = refMap.get(id);
    require(ref != null && ref != MJIEnv.NULL, description + " has no non-null JPF mapping");
    return ref;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException("[HPROF-JPF] smoke validation failed: " + message);
  }
}
