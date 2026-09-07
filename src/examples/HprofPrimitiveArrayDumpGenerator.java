import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofPrimitiveArrayDumpGenerator {
  private static volatile HprofPrimitiveArrayGraph.PrimitiveArrayGraph root;

  private HprofPrimitiveArrayDumpGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofPrimitiveArrayDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    HprofPrimitiveArrayGraph.PrimitiveArrayGraph graph =
        new HprofPrimitiveArrayGraph.PrimitiveArrayGraph();
    graph.marker = 66;
    graph.booleans = new boolean[] {true, false, true};
    graph.bytes = new byte[] {-7, 0, 100};
    graph.chars = new char[] {'A', '\u03A9', '\u0000'};
    graph.shorts = new short[] {-1234, 0, 30000};
    graph.ints = new int[] {-123456789, 0, 123456789};
    graph.longs = new long[] {-0x123456789ABCDEFL, 0L, 0x123456789ABCDEFL};
    graph.floats = new float[] {-13.25f, 0.0f, 13.25f};
    graph.doubles = new double[] {-12345.125, 0.0, 12345.125};
    root = graph;

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote primitive-array heap: " + output);
  }
}
