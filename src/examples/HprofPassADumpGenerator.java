import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofPassADumpGenerator {
  private static volatile HprofPassASmokeGraph.Foo root;
  private HprofPassADumpGenerator() {}
  public static void main(String[] args) throws IOException {
    if (args.length != 1) throw new IllegalArgumentException("usage: HprofPassADumpGenerator <output.hprof>");
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) throw new IOException("refusing to overwrite existing heap dump: " + output);
    HprofPassASmokeGraph.Bar bar = new HprofPassASmokeGraph.Bar();
    bar.number = 42;
    HprofPassASmokeGraph.Foo foo = new HprofPassASmokeGraph.Foo();
    foo.value = 123;
    foo.child = bar;
    foo.numbers = new int[] {10, 20, 30};
    root = foo;
    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote smoke heap: " + output);
  }
}
