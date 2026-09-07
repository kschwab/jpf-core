import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofPrimitiveScalarDumpGenerator {
  private static volatile HprofPrimitiveScalarGraph.PrimitiveGraph root;

  private HprofPrimitiveScalarDumpGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofPrimitiveScalarDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    HprofPrimitiveScalarGraph.PrimitiveValues values =
        new HprofPrimitiveScalarGraph.PrimitiveValues();
    values.booleanValue = true;
    values.byteValue = -7;
    values.charValue = '\u03A9';
    values.shortValue = 30000;
    values.intValue = 123456789;
    values.longValue = 0x123456789ABCDEFL;
    values.floatValue = 13.25f;
    values.doubleValue = -12345.125;

    HprofPrimitiveScalarGraph.PrimitiveGraph graph =
        new HprofPrimitiveScalarGraph.PrimitiveGraph();
    graph.marker = 55;
    graph.values = values;
    root = graph;

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote primitive-scalar heap: " + output);
  }
}
