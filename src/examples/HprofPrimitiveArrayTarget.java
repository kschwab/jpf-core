import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target for complete primitive-array reconstruction. */
public final class HprofPrimitiveArrayTarget {
  private HprofPrimitiveArrayTarget() {}

  public static void main(String[] args) {
    validateGraph("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] primitive arrays verified");
    System.gc();
    Verify.breakTransition("hprof-primitive-arrays-controlled-gc");
    validateGraph("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC primitive arrays verified");
  }

  private static void validateGraph(String phase) {
    HprofPrimitiveArrayGraph.PrimitiveArrayGraph graph = HprofPrimitiveArrayGraph.root;
    if (graph == null) throw new AssertionError(phase + ": root is null");
    if (graph.marker != 66) throw new AssertionError(phase + ": marker != 66");
    if (graph.booleans == null || graph.booleans.length != 3
        || !graph.booleans[0] || graph.booleans[1] || !graph.booleans[2]) {
      throw new AssertionError(phase + ": boolean[] mismatch");
    }
    if (graph.bytes == null || graph.bytes.length != 3
        || graph.bytes[0] != -7 || graph.bytes[1] != 0 || graph.bytes[2] != 100) {
      throw new AssertionError(phase + ": byte[] mismatch");
    }
    if (graph.chars == null || graph.chars.length != 3
        || graph.chars[0] != 'A' || graph.chars[1] != '\u03A9' || graph.chars[2] != '\u0000') {
      throw new AssertionError(phase + ": char[] mismatch");
    }
    if (graph.shorts == null || graph.shorts.length != 3
        || graph.shorts[0] != -1234 || graph.shorts[1] != 0 || graph.shorts[2] != 30000) {
      throw new AssertionError(phase + ": short[] mismatch");
    }
    if (graph.ints == null || graph.ints.length != 3
        || graph.ints[0] != -123456789 || graph.ints[1] != 0
        || graph.ints[2] != 123456789) {
      throw new AssertionError(phase + ": int[] mismatch");
    }
    if (graph.longs == null || graph.longs.length != 3
        || graph.longs[0] != -0x123456789ABCDEFL || graph.longs[1] != 0L
        || graph.longs[2] != 0x123456789ABCDEFL) {
      throw new AssertionError(phase + ": long[] mismatch");
    }
    if (graph.floats == null || graph.floats.length != 3
        || graph.floats[0] != -13.25f || graph.floats[1] != 0.0f
        || graph.floats[2] != 13.25f) {
      throw new AssertionError(phase + ": float[] mismatch");
    }
    if (graph.doubles == null || graph.doubles.length != 3
        || graph.doubles[0] != -12345.125 || graph.doubles[1] != 0.0
        || graph.doubles[2] != 12345.125) {
      throw new AssertionError(phase + ": double[] mismatch");
    }
  }
}
