import gov.nasa.jpf.vm.Verify;

/** Harmless modeled target that reads only statics reconstructed directly from HPROF. */
public final class HprofStaticStateTarget {
  private HprofStaticStateTarget() {}

  public static void main(String[] args) {
    validateState("pre-GC");
    System.out.println("[HPROF-JPF-MODEL] reconstructed static state verified");
    System.gc();
    Verify.breakTransition("hprof-static-state-controlled-gc");
    validateState("post-GC");
    System.out.println("[HPROF-JPF-MODEL] post-GC reconstructed static state verified");
  }

  private static void validateState(String phase) {
    if (!HprofStaticStateGraph.booleanValue) fail(phase, "booleanValue");
    if (HprofStaticStateGraph.byteValue != -7) fail(phase, "byteValue");
    if (HprofStaticStateGraph.charValue != '\u03A9') fail(phase, "charValue");
    if (HprofStaticStateGraph.shortValue != 30000) fail(phase, "shortValue");
    if (HprofStaticStateGraph.intValue != 123456789) fail(phase, "intValue");
    if (HprofStaticStateGraph.longValue != 0x123456789ABCDEFL) fail(phase, "longValue");
    if (HprofStaticStateGraph.floatValue != 13.25f) fail(phase, "floatValue");
    if (HprofStaticStateGraph.doubleValue != -12345.125) fail(phase, "doubleValue");
    if (HprofStaticStateGraph.payload == null || HprofStaticStateGraph.payload.id != 4242) {
      fail(phase, "payload");
    }
    if (HprofStaticStateGraph.payloadAlias != HprofStaticStateGraph.payload) {
      fail(phase, "payloadAlias");
    }
    if (HprofStaticStateGraph.nullable != null) fail(phase, "nullable");
    int[] numbers = HprofStaticStateGraph.numbers;
    if (numbers == null || numbers.length != 3
        || numbers[0] != 4 || numbers[1] != 5 || numbers[2] != 6) {
      fail(phase, "numbers");
    }
  }

  private static void fail(String phase, String field) {
    throw new AssertionError(phase + ": static " + field + " mismatch");
  }
}
