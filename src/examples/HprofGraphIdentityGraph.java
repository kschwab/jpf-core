/** Graph definitions shared by the real-JVM identity dump generator and modeled JPF target. */
public final class HprofGraphIdentityGraph {
  static Graph root;

  private HprofGraphIdentityGraph() {}

  public static final class Node {
    int id;
    Node next;
  }

  public static final class Graph {
    int marker;
    Node left;
    Node right;
    Node nullable;
  }
}
