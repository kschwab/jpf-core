/** Primitive-scalar definitions shared by HotSpot and modeled JPF. */
public final class HprofPrimitiveScalarGraph {
  static PrimitiveGraph root;

  private HprofPrimitiveScalarGraph() {}

  static final class PrimitiveValues {
    boolean booleanValue;
    byte byteValue;
    char charValue;
    short shortValue;
    int intValue;
    long longValue;
    float floatValue;
    double doubleValue;
  }

  static final class PrimitiveGraph {
    int marker;
    PrimitiveValues values;
  }
}
