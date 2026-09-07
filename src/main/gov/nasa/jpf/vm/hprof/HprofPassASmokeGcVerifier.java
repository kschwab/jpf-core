package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.VM;

import java.util.Map;

/** Smoke-only observer that verifies the imported graph after each observed JPF GC. */
public final class HprofPassASmokeGcVerifier {
  private static final String GRAPH_CLASS = "HprofPassASmokeGraph";
  private static final String FOO_CLASS = "HprofPassASmokeGraph$Foo";
  private static final String BAR_CLASS = "HprofPassASmokeGraph$Bar";
  private static final String ROOT_FIELD = "root";

  private final int fooRef;
  private final int barRef;
  private final int arrayRef;
  private int begunCycles;
  private int completedCycles;

  private HprofPassASmokeGcVerifier(int fooRef, int barRef, int arrayRef) {
    this.fooRef = fooRef;
    this.barRef = barRef;
    this.arrayRef = arrayRef;
  }

  public static HprofPassASmokeGcVerifier create(
      VM vm, JpfHeapImporter.ImportResult result, int fooRef) {
    require(fooRef != MJIEnv.NULL, "Foo reference is JPF NULL");
    ElementInfo foo = requireElement(vm, fooRef, FOO_CLASS, "Foo");
    int barRef = foo.getReferenceField("child");
    int arrayRef = foo.getReferenceField("numbers");
    require(barRef != MJIEnv.NULL, "Foo.child is JPF NULL before GC");
    require(arrayRef != MJIEnv.NULL, "Foo.numbers is JPF NULL before GC");

    Map<Long, Integer> refMap = result.getRefMap();
    require(refMap.size() == 3, "expected exactly three imported mappings");
    require(refMap.containsValue(fooRef), "Foo reference is absent from ImportResult");
    require(refMap.containsValue(barRef), "Bar reference is absent from ImportResult");
    require(refMap.containsValue(arrayRef), "int[] reference is absent from ImportResult");
    require(fooRef != barRef && fooRef != arrayRef && barRef != arrayRef,
        "imported references are not distinct");
    return new HprofPassASmokeGcVerifier(fooRef, barRef, arrayRef);
  }

  public void gcBegin() {
    begunCycles++;
    System.out.println("[HPROF-JPF] GC begin: cycle=" + begunCycles);
  }

  public void gcEnd(VM vm) {
    completedCycles++;
    require(completedCycles <= begunCycles, "GC end observed without matching GC begin");
    verifyPreservedGraph(vm);
    System.out.println("[HPROF-JPF] GC end: cycle=" + completedCycles);
    if (completedCycles == 1) {
      System.out.println("[HPROF-JPF] controlled GC verified: cycles=1"
          + " Foo=" + fooRef + " Bar=" + barRef + " int[]=" + arrayRef);
    } else {
      System.out.println("[HPROF-JPF] rooted graph verified after additional GC: cycle="
          + completedCycles);
    }
  }

  private void verifyPreservedGraph(VM vm) {
    ClassInfo graphClass = ClassLoaderInfo.getSystemResolvedClassInfo(GRAPH_CLASS);
    StaticElementInfo statics = graphClass.getStaticElementInfo();
    require(statics != null, "smoke graph static storage disappeared during GC");
    require(statics.getReferenceField(ROOT_FIELD) == fooRef,
        "static root no longer contains the original Foo reference");

    ElementInfo foo = requireElement(vm, fooRef, FOO_CLASS, "Foo");
    ElementInfo bar = requireElement(vm, barRef, BAR_CLASS, "Bar");
    ElementInfo array = requireElement(vm, arrayRef, "[I", "int[]");
    require(foo.getIntField("value") == 123, "Foo.value did not survive GC");
    require(foo.getReferenceField("child") == barRef,
        "Foo.child no longer contains the original Bar reference");
    require(foo.getReferenceField("numbers") == arrayRef,
        "Foo.numbers no longer contains the original int[] reference");
    require(bar.getIntField("number") == 42, "Bar.number did not survive GC");
    require(array.isArray() && array.arrayLength() == 3, "int[] shape did not survive GC");
    require(array.getIntElement(0) == 10 && array.getIntElement(1) == 20
        && array.getIntElement(2) == 30, "int[] payload did not survive GC");
  }

  private static ElementInfo requireElement(VM vm, int ref, String className, String description) {
    ElementInfo element = vm.getHeap().get(ref);
    require(element != null, description + " was collected");
    require(className.equals(element.getClassInfo().getName()),
        description + " reference now has class " + element.getClassInfo().getName());
    return element;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] smoke GC verification failed: " + message);
    }
  }
}
