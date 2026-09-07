import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Run only on a real HotSpot JVM. Never use this class as a JPF target. */
public final class HprofInheritanceDumpGenerator {
  private static volatile HprofInheritanceGraph.InheritanceGraph root;

  private HprofInheritanceDumpGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofInheritanceDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    HprofInheritanceGraph.Ref shared = new HprofInheritanceGraph.Ref();
    shared.id = 7;

    HprofInheritanceGraph.Derived derived = new HprofInheritanceGraph.Derived();
    derived.baseValue = 11;
    derived.derivedValue = 22;
    derived.baseRef = shared;
    derived.derivedRef = shared;

    HprofInheritanceGraph.HiddenDerived hidden =
        new HprofInheritanceGraph.HiddenDerived();
    ((HprofInheritanceGraph.HiddenBase) hidden).value = 31;
    hidden.value = 32;

    HprofInheritanceGraph.InheritanceGraph graph =
        new HprofInheritanceGraph.InheritanceGraph();
    graph.marker = 99;
    graph.derived = derived;
    graph.hidden = hidden;
    root = graph;

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] wrote inheritance heap: " + output);
  }
}
