package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.VM;

/** Host-side validation for G.3 supplemental class-lifecycle continuation. */
public final class HprofClassLifecycleValidator implements HprofTestSupport {
  private static final String SUBJECT = "HprofClassInitState$InitSubject";
  private static final String EFFECTS = "HprofClassInitState$InitEffects";

  private boolean initiallyInitialized;
  private int completedGcCycles;

  @Override
  public void initialize(VM vm, HprofView view, JpfHeapImporter.ImportResult result, int rootRef) {
    require(rootRef == MJIEnv.NULL, "lifecycle fixture unexpectedly used a test root");
    require(result.getAllocatedObjects() == 0 && result.getAllocatedArrays() == 0
        && result.getRefMap().isEmpty(), "lifecycle fixture unexpectedly imported heap objects");
    require(result.getStaticPrimitiveFields() == 2 && result.getStaticReferences() == 0,
        "unexpected lifecycle fixture import counts");

    ClassInfo subject = ClassLoaderInfo.getSystemResolvedClassInfo(SUBJECT);
    StaticElementInfo subjectStatics = requireStatics(subject);
    StaticElementInfo effectsStatics = requireStatics(
        ClassLoaderInfo.getSystemResolvedClassInfo(EFFECTS));
    int status = subjectStatics.getStatus();
    require(status == ClassInfo.UNINITIALIZED || status == ClassInfo.INITIALIZED,
        "unexpected InitSubject status " + status);
    initiallyInitialized = status == ClassInfo.INITIALIZED;
    int effects = effectsStatics.getIntField(declaredStaticField(EFFECTS, "effectCount"));
    require(effects == (initiallyInitialized ? 1 : 0),
        "captured effectCount does not match lifecycle state");
    require(subjectStatics.getIntField(declaredStaticField(SUBJECT, "value")) == 0,
        "captured InitSubject.value is not zero");
    System.out.println("[HPROF-JPF] lifecycle pre-use class=" + SUBJECT + " state="
        + (initiallyInitialized ? "INITIALIZED" : "UNINITIALIZED")
        + " status=" + status + " effectCount=" + effects);
  }

  @Override
  public void gcEnd(VM vm) {
    completedGcCycles++;
    ClassInfo subject = ClassLoaderInfo.getSystemResolvedClassInfo(SUBJECT);
    require(requireStatics(subject).getStatus() == ClassInfo.INITIALIZED,
        "InitSubject was not initialized after modeled active use");
    StaticElementInfo effects = requireStatics(
        ClassLoaderInfo.getSystemResolvedClassInfo(EFFECTS));
    require(effects.getIntField(declaredStaticField(EFFECTS, "effectCount")) == 1,
        "<clinit> did not execute exactly once across continuation");
    if (completedGcCycles == 1) {
      System.out.println("[HPROF-JPF] lifecycle post-use verified: initial="
          + (initiallyInitialized ? "INITIALIZED" : "UNINITIALIZED")
          + " final=INITIALIZED effectCount=1");
    }
  }

  private static StaticElementInfo requireStatics(ClassInfo ci) {
    StaticElementInfo statics = ci.getStaticElementInfo();
    require(statics != null, "missing static storage for " + ci.getName());
    return statics;
  }

  private static FieldInfo declaredStaticField(String className, String fieldName) {
    FieldInfo field = ClassLoaderInfo.getSystemResolvedClassInfo(className)
        .getDeclaredStaticField(fieldName);
    require(field != null, "missing static field " + className + "." + fieldName);
    return field;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] lifecycle validation failed: " + message);
    }
  }
}
