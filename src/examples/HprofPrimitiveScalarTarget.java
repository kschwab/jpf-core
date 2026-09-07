import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target for complete primitive-scalar reconstruction. */
public final class HprofPrimitiveScalarTarget {
  private HprofPrimitiveScalarTarget() {}

  public static void main(String[] args) {
    validateGraph("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] primitive scalar values verified");
    System.gc();
    Verify.breakTransition("hprof-primitive-scalars-controlled-gc");
    validateGraph("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC primitive scalar values verified");
  }

  private static void validateGraph(String phase) {
    HprofPrimitiveScalarGraph.PrimitiveGraph graph = HprofPrimitiveScalarGraph.root;
    if (graph == null) throw new AssertionError(phase + ": root is null");
    if (graph.marker != 55) throw new AssertionError(phase + ": marker != 55");
    HprofPrimitiveScalarGraph.PrimitiveValues values = graph.values;
    if (values == null) throw new AssertionError(phase + ": values is null");
    if (!values.booleanValue) throw new AssertionError(phase + ": booleanValue != true");
    if (values.byteValue != -7) throw new AssertionError(phase + ": byteValue != -7");
    if (values.charValue != '\u03A9') throw new AssertionError(phase + ": charValue != U+03A9");
    if (values.shortValue != 30000) throw new AssertionError(phase + ": shortValue != 30000");
    if (values.intValue != 123456789) throw new AssertionError(phase + ": intValue mismatch");
    if (values.longValue != 0x123456789ABCDEFL) {
      throw new AssertionError(phase + ": longValue mismatch");
    }
    if (values.floatValue != 13.25f) throw new AssertionError(phase + ": floatValue mismatch");
    if (values.doubleValue != -12345.125) {
      throw new AssertionError(phase + ": doubleValue mismatch");
    }
  }
}
