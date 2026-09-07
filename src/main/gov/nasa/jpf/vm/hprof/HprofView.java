package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Heap;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal, HAHA-compatible view over a parsed HPROF snapshot.
 * Works with HAHA/perflib 2.x (no Snapshot#getClasses()).
 */
public final class HprofView {
  public final Map<Long, ClassObj>      classes    = new HashMap<>();
  public final Map<Long, Instance>      instances  = new HashMap<>();
  public final Map<Long, ArrayInstance> objArrays  = new HashMap<>();
  public final Map<Long, ArrayInstance> primArrays = new HashMap<>();
  // Optional: record String instance ids -> (fill values later if needed)
  public final Map<Long, String>        strings    = new HashMap<>();

  private HprofView() {}

  public static HprofView from(Snapshot s) {
    // Ensure cross references are resolved before traversal
    s.resolveClasses();
    s.resolveReferences();

    HprofView v = new HprofView();

    for (Heap heap : s.getHeaps()) {
      // classes
      for (ClassObj c : heap.getClasses()) {
        v.classes.put(c.getId(), c);
      }

      // instances (including arrays)
      for (Instance inst : heap.getInstances()) {
        if (inst instanceof ArrayInstance) {
          ArrayInstance a = (ArrayInstance) inst;
          if (a.getArrayType() == Type.OBJECT) v.objArrays.put(a.getId(), a);
          else v.primArrays.put(a.getId(), a);
        } else {
          v.instances.put(inst.getId(), inst);
          ClassObj co = inst.getClassObj();
          if (co != null && "java.lang.String".equals(co.getClassName())) {
            v.strings.put(inst.getId(), null); // decode later if needed
          }
        }
      }
    }

    return v;
  }
}
