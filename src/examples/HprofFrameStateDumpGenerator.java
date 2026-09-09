import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Real-HotSpot producer for the minimum frame-state fixture. */
public final class HprofFrameStateDumpGenerator {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("expected explicit HPROF output path");
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) throw new IllegalStateException("refusing to overwrite: " + output);
    Files.createDirectories(output.getParent());

    HprofFrameStateFixture.Marker marker = new HprofFrameStateFixture.Marker(7);
    HprofFrameStateFixture.captureMarker = marker;
    Thread worker = new Thread(HprofFrameStateFixture::checkpointMethod, "hprof-frame-capture");
    worker.start();
    while (!HprofFrameStateCaptureBarrier.ready) Thread.onSpinWait();

    // The worker's local is now the sole application reference to Marker.
    HprofFrameStateFixture.captureMarker = null;
    marker = null;

    HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    bean.dumpHeap(output.toString(), true);
    HprofFrameStateCaptureBarrier.captured = true;
    worker.join();
    System.out.println("[HPROF-JPF] wrote frame-state heap: " + output);
  }
}
