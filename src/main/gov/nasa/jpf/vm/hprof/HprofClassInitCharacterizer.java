package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.RootType;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.Type;
import java.io.File;
import java.util.Map;

/** Characterizes the class-initialization information absent from standard HPROF. */
public final class HprofClassInitCharacterizer {
  private static final String SUBJECT = "HprofClassInitState$InitSubject";

  private HprofClassInitCharacterizer() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("usage: HprofClassInitCharacterizer <uninitialized.hprof> <initialized.hprof>");
    }
    SubjectMetadata before = inspect(new File(args[0]), "loaded-not-initialized");
    SubjectMetadata after = inspect(new File(args[1]), "initialized");

    require(before.value == 0 && after.value == 0,
        "InitSubject.value must be identical in both captures");
    require(before.initLock && !after.initLock,
        "expected the current HotSpot implementation to expose <init_lock> only before initialization");
    require(before.systemClassRoot == after.systemClassRoot,
        "SYSTEM_CLASS root presence differs between captures");
    require(before.superClassName.equals(after.superClassName),
        "InitSubject superclass metadata differs");

    System.out.println("[HPROF-JPF] class-init ambiguity verified: "
        + "loaded-not-initialized.value=0 initialized.value=0");
    System.out.println("[HPROF-JPF] declared Java static value is ambiguous; standard CLASS_DUMP has no initialization-status field");
    System.out.println("[HPROF-JPF] HotSpot-specific observation: <init_lock> present before initialization and absent after it");
    System.out.println("[HPROF-JPF] class-init characterization result: CASE_B");
  }

  private static SubjectMetadata inspect(File file, String label) throws Exception {
    Snapshot snapshot = HprofSnapshotLoader.load(file);
    HprofView view = HprofView.from(snapshot);
    ClassObj subject = null;
    for (ClassObj candidate : view.classes.values()) {
      if (SUBJECT.equals(candidate.getClassName())) {
        require(subject == null, "multiple ClassObj records for " + SUBJECT + " in " + label);
        subject = candidate;
      }
    }
    require(subject != null, "missing ClassObj for " + SUBJECT + " in " + label);

    Map<Field, Object> statics = subject.getStaticFieldValues();
    Object value = null;
    boolean foundValue = false;
    for (Map.Entry<Field, Object> entry : statics.entrySet()) {
      if ("value".equals(entry.getKey().getName())) {
        require(entry.getKey().getType() == Type.INT, "InitSubject.value is not Type.INT");
        value = entry.getValue();
        foundValue = true;
      }
    }
    require(foundValue && value instanceof Integer, "missing boxed InitSubject.value in " + label);

    boolean systemClassRoot = false;
    for (RootObj root : snapshot.getGCRoots()) {
      if (root.getRootType() == RootType.SYSTEM_CLASS) {
        Instance referred = root.getReferredInstance();
        if (referred != null && referred.getId() == subject.getId()) {
          systemClassRoot = true;
          break;
        }
      }
    }
    for (Map.Entry<Field, Object> entry : statics.entrySet()) {
      System.out.println("[HPROF-JPF] HAHA static entry: capture=" + label
          + " name=" + entry.getKey().getName() + " type=" + entry.getKey().getType()
          + " valueClass=" + (entry.getValue() == null ? "null" : entry.getValue().getClass().getName()));
    }
    boolean initLock = false;
    for (Field field : statics.keySet()) {
      if ("<init_lock>".equals(field.getName())) {
        require(field.getType() == Type.OBJECT, "<init_lock> is not Type.OBJECT");
        initLock = true;
      }
    }
    String superName = subject.getSuperClassObj() == null
        ? "<none>" : subject.getSuperClassObj().getClassName();
    System.out.println("[HPROF-JPF] HAHA class-init metadata: capture=" + label
        + " class=" + subject.getClassName() + " value=" + value
        + " staticFields=" + statics.size() + " systemClassRoot=" + systemClassRoot
        + " superclass=" + superName);
    return new SubjectMetadata((Integer) value, statics.size(), systemClassRoot, superName, initLock);
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] class-init characterization failed: " + message);
    }
  }

  private static final class SubjectMetadata {
    final int value;
    final int staticFieldCount;
    final boolean systemClassRoot;
    final String superClassName;
    final boolean initLock;

    SubjectMetadata(int value, int staticFieldCount, boolean systemClassRoot, String superClassName, boolean initLock) {
      this.value = value;
      this.staticFieldCount = staticFieldCount;
      this.systemClassRoot = systemClassRoot;
      this.superClassName = superClassName;
      this.initLock = initLock;
    }
  }
}
