import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target. HPROF loading is performed by HprofHeapBootstrap. */
public final class HprofPassASmokeTarget {
  private HprofPassASmokeTarget() {}

  public static void main(String[] args) {
    validateGraph("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] pre-GC graph verified");
    System.out.println("[HPROF-JPF-MODEL] requesting deliberate JPF GC");
    System.gc();
    Verify.breakTransition("hprof-pass-c2-controlled-gc");
    validateGraph("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC reconstructed graph verified");
  }

  private static void validateGraph(String phase) {
    HprofPassASmokeGraph.Foo foo = HprofPassASmokeGraph.root;
    if (foo == null) throw new AssertionError(phase + ": imported root is null");
    if (foo.value != 123) throw new AssertionError(phase + ": Foo.value != 123");
    if (foo.child == null) throw new AssertionError(phase + ": Foo.child is null");
    if (foo.child.number != 42) throw new AssertionError(phase + ": Bar.number != 42");
    if (foo.numbers == null) throw new AssertionError(phase + ": Foo.numbers is null");
    if (foo.numbers.length != 3) throw new AssertionError(phase + ": Foo.numbers.length != 3");
    if (foo.numbers[0] != 10 || foo.numbers[1] != 20 || foo.numbers[2] != 30) {
      throw new AssertionError(phase + ": Foo.numbers != {10,20,30}");
    }
  }
}
