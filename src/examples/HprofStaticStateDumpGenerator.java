import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofStaticStateDumpGenerator {
  private HprofStaticStateDumpGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofStaticStateDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    HprofStaticStateGraph.booleanValue = true;
    HprofStaticStateGraph.byteValue = -7;
    HprofStaticStateGraph.charValue = '\u03A9';
    HprofStaticStateGraph.shortValue = 30000;
    HprofStaticStateGraph.intValue = 123456789;
    HprofStaticStateGraph.longValue = 0x123456789ABCDEFL;
    HprofStaticStateGraph.floatValue = 13.25f;
    HprofStaticStateGraph.doubleValue = -12345.125;

    HprofStaticStateGraph.Payload payload = new HprofStaticStateGraph.Payload();
    payload.id = 4242;
    HprofStaticStateGraph.payload = payload;
    HprofStaticStateGraph.payloadAlias = payload;
    HprofStaticStateGraph.nullable = null;
    HprofStaticStateGraph.numbers = new int[] {4, 5, 6};

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote static-state heap: " + output);
  }
}
