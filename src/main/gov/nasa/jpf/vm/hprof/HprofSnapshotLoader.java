package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.HprofParser;
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer;

import java.io.File;

public final class HprofSnapshotLoader {
  public static Snapshot load(File hprof) throws Exception {
    // HAHA reads standard HPROF (32/64-bit id size handled from header)
    MemoryMappedFileBuffer buf = new MemoryMappedFileBuffer(hprof);
    HprofParser parser = new HprofParser(buf);
    return parser.parse();
  }
}
