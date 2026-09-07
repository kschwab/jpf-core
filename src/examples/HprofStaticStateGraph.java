/** Static-state holder shared by HotSpot and modeled JPF. Contains no class initializer. */
public final class HprofStaticStateGraph {
  static boolean booleanValue;
  static byte byteValue;
  static char charValue;
  static short shortValue;
  static int intValue;
  static long longValue;
  static float floatValue;
  static double doubleValue;

  static Payload payload;
  static Payload payloadAlias;
  static Payload nullable;
  static int[] numbers;

  private HprofStaticStateGraph() {}

  static final class Payload {
    int id;
  }
}
