/** Primitive-array graph shared by HotSpot and modeled JPF. */
public final class HprofPrimitiveArrayGraph {
  static PrimitiveArrayGraph root;

  private HprofPrimitiveArrayGraph() {}

  static final class PrimitiveArrayGraph {
    int marker;
    boolean[] booleans;
    byte[] bytes;
    char[] chars;
    short[] shorts;
    int[] ints;
    long[] longs;
    float[] floats;
    double[] doubles;
  }
}
