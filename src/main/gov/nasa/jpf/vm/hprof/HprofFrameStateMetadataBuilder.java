package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.RootType;
import com.squareup.haha.perflib.Snapshot;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Correlates the known synthetic frame values with the captured HPROF Marker identity. */
public final class HprofFrameStateMetadataBuilder {
  public static void main(String[] args) throws Exception {
    Snapshot snapshot = HprofSnapshotLoader.load(new File(args[0]));
    HprofView view = HprofView.from(snapshot);
    ClassInstance marker = null;
    for (Instance instance : view.instances.values()) {
      if (instance instanceof ClassInstance
          && "HprofFrameStateFixture$Marker".equals(instance.getClassObj().getClassName())) {
        if (marker != null) {
          throw new IllegalStateException("multiple markers");
        }
        marker = (ClassInstance) instance;
      }
    }
    if (marker == null) {
      throw new IllegalStateException("missing marker");
    }

    boolean javaLocal = false;
    for (RootObj root : snapshot.getGCRoots()) {
      if (root.getId() == marker.getId() && root.getRootType() == RootType.JAVA_LOCAL) {
        javaLocal = true;
      }
    }
    if (!javaLocal) {
      throw new IllegalStateException("marker is not JAVA_LOCAL");
    }

    String text =
        "execution.version=1\n"
            + "thread.1.logical_id=1\n"
            + "thread.1.state=RUNNING\n"
            + "frame.1.class=HprofFrameStateFixture\n"
            + "frame.1.method=checkpointMethod\n"
            + "frame.1.descriptor=()V\n"
            + "frame.1.bytecode_offset=22\n"
            + "frame.1.local_count=3\n"
            + "frame.1.local.0=INT:5\n"
            + "frame.1.local.1=REFERENCE_HPROF:0x"
            + Long.toHexString(marker.getId())
            + "\n"
            + "frame.1.local.2=INT:15\n";
    Files.writeString(Path.of(args[1]), text, StandardCharsets.UTF_8);
    System.out.printf(
        "[HPROF-JPF] execution metadata built: thread=1 method=checkpointMethod()V"
            + " pc=22 locals={INT:5,REF:0x%x,INT:15} operandDepth=0%n",
        marker.getId());
  }
}
