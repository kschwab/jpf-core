import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target for inherited and hidden field reconstruction. */
public final class HprofInheritanceTarget {
  private HprofInheritanceTarget() {}

  public static void main(String[] args) {
    validateGraph("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] inheritance field layout verified");
    System.gc();
    Verify.breakTransition("hprof-inheritance-controlled-gc");
    validateGraph("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC inheritance field layout verified");
  }

  private static void validateGraph(String phase) {
    HprofInheritanceGraph.InheritanceGraph graph = HprofInheritanceGraph.root;
    if (graph == null) throw new AssertionError(phase + ": root is null");
    if (graph.marker != 99) throw new AssertionError(phase + ": marker != 99");
    if (graph.derived == null) throw new AssertionError(phase + ": derived is null");
    if (graph.derived.baseValue != 11) {
      throw new AssertionError(phase + ": Base.baseValue != 11");
    }
    if (graph.derived.derivedValue != 22) {
      throw new AssertionError(phase + ": Derived.derivedValue != 22");
    }
    if (graph.derived.baseRef == null || graph.derived.baseRef.id != 7) {
      throw new AssertionError(phase + ": inherited Base.baseRef is invalid");
    }
    if (graph.derived.derivedRef != graph.derived.baseRef) {
      throw new AssertionError(phase + ": inherited/subclass reference alias was lost");
    }
    if (graph.hidden == null) throw new AssertionError(phase + ": hidden is null");
    if (((HprofInheritanceGraph.HiddenBase) graph.hidden).value != 31) {
      throw new AssertionError(phase + ": HiddenBase.value != 31");
    }
    if (graph.hidden.value != 32) {
      throw new AssertionError(phase + ": HiddenDerived.value != 32");
    }
  }
}
