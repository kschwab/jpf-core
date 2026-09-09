import gov.nasa.jpf.vm.Verify;

/** Modeled continuation test for supplemental class-lifecycle metadata V1. */
public final class HprofClassLifecycleTarget {
  private HprofClassLifecycleTarget() {}

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new AssertionError("expected lifecycle argument");
    }
    boolean capturedInitialized = "INITIALIZED".equals(args[0]);
    if (!capturedInitialized && !"UNINITIALIZED".equals(args[0])) {
      throw new AssertionError("unknown lifecycle argument: " + args[0]);
    }

    int expectedBefore = capturedInitialized ? 1 : 0;
    if (HprofClassInitState.InitEffects.effectCount != expectedBefore) {
      throw new AssertionError("captured effectCount mismatch before active use");
    }

    int first = HprofClassInitState.InitSubject.value;
    if (first != 0 || HprofClassInitState.InitEffects.effectCount != 1) {
      throw new AssertionError("first active use produced incorrect initialization behavior");
    }
    int second = HprofClassInitState.InitSubject.value;
    if (second != 0 || HprofClassInitState.InitEffects.effectCount != 1) {
      throw new AssertionError("second active use reran <clinit>");
    }

    System.out.println("[HPROF-JPF-MODEL] " + args[0]
        + " lifecycle continuation verified");
    System.gc();
    Verify.breakTransition("hprof-class-lifecycle-controlled-gc");
  }
}
