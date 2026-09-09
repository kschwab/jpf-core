import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Real-HotSpot root fixture with distinct static, Java-local, monitor, and thread identities. */
public final class HprofRootCharacterizationDumpGenerator {
  static final Marker staticMarker = new Marker(1);
  static volatile boolean ready;
  static volatile boolean captured;
  static Thread worker;

  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("usage: HprofRootCharacterizationDumpGenerator <output.hprof>");
    Path output=Path.of(args[0]).toAbsolutePath().normalize();
    if(Files.exists(output))throw new IOException("refusing to overwrite HPROF: "+output);
    worker=new Thread(HprofRootCharacterizationDumpGenerator::holdRoots,"hprof-root-worker");
    worker.start();
    while(!ready) Thread.onSpinWait();
    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(output.toString(),true);
    captured=true; worker.join();
    System.out.println("[HPROF-JPF] wrote root characterization heap: "+output);
  }

  private static void holdRoots(){
    Marker javaLocal=new Marker(2);
    Marker monitor=new Marker(3);
    synchronized(monitor){
      ready=true;
      while(!captured) Thread.onSpinWait();
      if(javaLocal.kind+monitor.kind==0)throw new AssertionError();
    }
  }

  static final class Marker { final int kind; Marker(int kind){this.kind=kind;} }
}
