import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofGraphIdentityDumpGenerator {
  private static volatile HprofGraphIdentityGraph.Graph root;

  private HprofGraphIdentityDumpGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofGraphIdentityDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    HprofGraphIdentityGraph.Node a = new HprofGraphIdentityGraph.Node();
    a.id = 1;
    HprofGraphIdentityGraph.Node b = new HprofGraphIdentityGraph.Node();
    b.id = 2;
    a.next = b;
    b.next = a;

    HprofGraphIdentityGraph.Graph graph = new HprofGraphIdentityGraph.Graph();
    graph.marker = 99;
    graph.left = a;
    graph.right = a;
    graph.nullable = null;
    root = graph;

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote graph-identity heap: " + output);
  }
}
