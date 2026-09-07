import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofObjectArrayDumpGenerator {
  private static volatile HprofObjectArrayGraph.ArrayGraph root;

  private HprofObjectArrayDumpGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofObjectArrayDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    HprofObjectArrayGraph.Node a = new HprofObjectArrayGraph.Node();
    a.id = 1;
    HprofObjectArrayGraph.Node b = new HprofObjectArrayGraph.Node();
    b.id = 2;
    HprofObjectArrayGraph.Node[] nodes = new HprofObjectArrayGraph.Node[4];
    nodes[0] = a;
    nodes[1] = b;
    nodes[2] = a;
    nodes[3] = null;

    a.peer = b;
    b.peer = a;
    a.links = nodes;
    b.links = null;

    HprofObjectArrayGraph.ArrayGraph graph = new HprofObjectArrayGraph.ArrayGraph();
    graph.marker = 77;
    graph.anchor = a;
    graph.nodes = nodes;
    root = graph;

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote object-array heap: " + output);
  }
}
