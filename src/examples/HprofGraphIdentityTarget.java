import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target for null, alias, and cyclic graph identity semantics. */
public final class HprofGraphIdentityTarget {
  private HprofGraphIdentityTarget() {}

  public static void main(String[] args) {
    validateGraph("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] pre-GC graph identity semantics verified");
    System.gc();
    Verify.breakTransition("hprof-graph-identity-controlled-gc");
    validateGraph("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC graph identity semantics verified");
    System.out.println("[HPROF-JPF-MODEL] graph identity semantics verified");
  }

  private static void validateGraph(String phase) {
    HprofGraphIdentityGraph.Graph graph = HprofGraphIdentityGraph.root;
    if (graph == null) throw new AssertionError(phase + ": root is null");
    if (graph.marker != 99) throw new AssertionError(phase + ": Graph.marker != 99");
    if (graph.left == null) throw new AssertionError(phase + ": Graph.left is null");
    if (graph.right == null) throw new AssertionError(phase + ": Graph.right is null");
    if (graph.left != graph.right) throw new AssertionError(phase + ": alias was not preserved");
    if (graph.nullable != null) throw new AssertionError(phase + ": nullable is not null");
    if (graph.left.id != 1) throw new AssertionError(phase + ": Node(a).id != 1");
    if (graph.left.next == null || graph.left.next.id != 2) {
      throw new AssertionError(phase + ": Node(a).next is not Node(b)");
    }
    if (graph.left.next.next != graph.left) {
      throw new AssertionError(phase + ": Node(a) -> Node(b) -> Node(a) cycle was not preserved");
    }
  }
}
