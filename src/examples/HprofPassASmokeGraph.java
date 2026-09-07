/** Class definitions shared by the real-JVM dump generator and modeled JPF target. */
public final class HprofPassASmokeGraph {
  static Foo root;

  private HprofPassASmokeGraph() {}
  public static final class Bar { int number; }
  public static final class Foo { int value; Bar child; int[] numbers; }
}
