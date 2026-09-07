import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target for object-array topology reconstruction. */
public final class HprofObjectArrayTarget {
  private HprofObjectArrayTarget() {}

  public static void main(String[] args) {
    validateGraph("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] pre-GC object-array topology verified");
    System.gc();
    Verify.breakTransition("hprof-object-array-controlled-gc");
    validateGraph("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC object-array topology verified");
    System.out.println("[HPROF-JPF-MODEL] object-array topology verified");
  }

  private static void validateGraph(String phase) {
    HprofObjectArrayGraph.ArrayGraph graph = HprofObjectArrayGraph.root;
    if (graph == null) throw new AssertionError(phase + ": root is null");
    if (graph.marker != 77) throw new AssertionError(phase + ": marker != 77");
    if (graph.nodes == null || graph.nodes.length != 4) {
      throw new AssertionError(phase + ": nodes is not Node[4]");
    }
    if (graph.nodes[0] == null || graph.nodes[0].id != 1) {
      throw new AssertionError(phase + ": nodes[0] is not Node(a)");
    }
    if (graph.nodes[1] == null || graph.nodes[1].id != 2) {
      throw new AssertionError(phase + ": nodes[1] is not Node(b)");
    }
    if (graph.nodes[2] != graph.nodes[0]) {
      throw new AssertionError(phase + ": nodes[2] does not alias nodes[0]");
    }
    if (graph.nodes[3] != null) throw new AssertionError(phase + ": nodes[3] is not null");
    if (graph.anchor != graph.nodes[0]) {
      throw new AssertionError(phase + ": anchor does not alias nodes[0]");
    }
    if (graph.nodes[0].peer != graph.nodes[1]
        || graph.nodes[1].peer != graph.nodes[0]) {
      throw new AssertionError(phase + ": Node peer cycle was not preserved");
    }
    if (graph.nodes[0].links != graph.nodes) {
      throw new AssertionError(phase + ": nodes -> Node(a) -> nodes cycle was not preserved");
    }
    if (graph.nodes[1].links != null) {
      throw new AssertionError(phase + ": Node(b).links is not null");
    }
  }
}
