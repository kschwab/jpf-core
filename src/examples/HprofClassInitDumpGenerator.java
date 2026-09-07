import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Produces one side of the G.2 ambiguity pair. */
public final class HprofClassInitDumpGenerator {
  private static final String SUBJECT = "HprofClassInitState$InitSubject";

  private HprofClassInitDumpGenerator() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !("uninitialized".equals(args[0]) || "initialized".equals(args[0]))) {
      throw new IllegalArgumentException(
          "usage: HprofClassInitDumpGenerator <uninitialized|initialized> <output.hprof>");
    }
    String state = args[0];
    Path output = Path.of(args[1]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    ClassLoader loader = HprofClassInitDumpGenerator.class.getClassLoader();
    Class.forName(SUBJECT, "initialized".equals(state), loader);
    int effects = HprofClassInitState.InitEffects.effectCount;
    int expectedEffects = "initialized".equals(state) ? 1 : 0;
    if (effects != expectedEffects) {
      throw new AssertionError(state + " capture observed effectCount=" + effects);
    }

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] HotSpot class-init capture: state=" + state
        + " subject.value=0 effectCount=" + effects + " path=" + output);
  }
}
