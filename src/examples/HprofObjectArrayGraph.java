/** Object-array graph definitions shared by the real-JVM generator and modeled JPF target. */
public final class HprofObjectArrayGraph {
  static ArrayGraph root;

  private HprofObjectArrayGraph() {}

  public static final class Node {
    int id;
    Node peer;
    Node[] links;
  }

  public static final class ArrayGraph {
    int marker;
    Node anchor;
    Node[] nodes;
  }
}
