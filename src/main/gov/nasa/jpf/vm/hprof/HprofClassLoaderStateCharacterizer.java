package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.Type;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Characterizes ordinary loader state and incidental class bytes in the H.1 capture. */
public final class HprofClassLoaderStateCharacterizer {
  private static final String DUPLICATE = "hprof/loader/Duplicate";
  private static final String LOADER = "HprofClassLoaderIdentityDumpGenerator$DuplicateLoader";

  private HprofClassLoaderStateCharacterizer() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException(
          "usage: HprofClassLoaderStateCharacterizer <loader-identity.hprof>");
    }
    Snapshot snapshot = HprofSnapshotLoader.load(new File(args[0]));
    HprofView view = HprofView.from(snapshot);
    List<ClassObj> duplicateClasses = new ArrayList<>(snapshot.findClasses(DUPLICATE));
    require(duplicateClasses.size() == 2, "expected two duplicate class definitions");

    List<LoaderState> states = new ArrayList<>();
    for (ClassObj duplicate : duplicateClasses) {
      Instance rawLoader = duplicate.getClassLoader();
      require(rawLoader instanceof ClassInstance, "defining loader is not a ClassInstance");
      require(LOADER.equals(rawLoader.getClassObj().getClassName()),
          "unexpected defining loader class " + rawLoader.getClassObj().getClassName());
      states.add(inspect((ClassInstance) rawLoader, view));
    }
    require(states.get(0).loaderId != states.get(1).loaderId, "loader IDs collided");
    require(states.get(0).byteArrayId != states.get(1).byteArrayId,
        "fixture loaders unexpectedly share one byte array");
    require(Arrays.equals(states.get(0).classBytes, states.get(1).classBytes),
        "fixture loaders retained different class definitions");

    for (LoaderState state : states) {
      System.out.printf("[HPROF-JPF] loader-state loaderHPROF=0x%x parent=null "
              + "labelType=java.lang.String byteArrayHPROF=0x%x byteLength=%d magic=CAFEBABE%n",
          state.loaderId, state.byteArrayId, state.classBytes.length);
    }
    System.out.println("[HPROF-JPF] loader ordinary state verified: parent=null label=String bytecode=byte[]");
    System.out.println("[HPROF-JPF] fixture class bytes retained incidentally in loader fields: identical=true");
    System.out.println("[HPROF-JPF] standard HPROF ClassObj has structural metadata but no classfile bytecode payload");
  }

  private static LoaderState inspect(ClassInstance loader, HprofView view) {
    Object parent = null;
    boolean foundParent = false;
    Instance label = null;
    ArrayInstance bytecode = null;
    for (ClassInstance.FieldValue value : loader.getValues()) {
      String name = value.getField().getName();
      if ("parent".equals(name)) {
        require(value.getField().getType() == Type.OBJECT, "loader parent is not OBJECT");
        parent = value.getValue();
        foundParent = true;
      } else if ("label".equals(name)) {
        require(value.getValue() instanceof Instance, "loader label is not an Instance");
        label = (Instance) value.getValue();
      } else if ("bytecode".equals(name)) {
        require(value.getValue() instanceof ArrayInstance, "loader bytecode is not ArrayInstance");
        bytecode = (ArrayInstance) value.getValue();
      }
    }
    require(foundParent && parent == null, "fixture loader parent is not represented as null");
    require(label != null && ("java.lang.String".equals(label.getClassObj().getClassName()) || "java/lang/String".equals(label.getClassObj().getClassName())),
        "loader label is not java.lang.String");
    require(bytecode != null && bytecode.getArrayType() == Type.BYTE,
        "loader bytecode is not Type.BYTE array");
    require(view.primArrays.get(bytecode.getId()) == bytecode,
        "loader byte array is not retained by HprofView");
    byte[] bytes = unbox(bytecode.getValues());
    require(bytes.length >= 4
            && (bytes[0] & 0xff) == 0xca && (bytes[1] & 0xff) == 0xfe
            && (bytes[2] & 0xff) == 0xba && (bytes[3] & 0xff) == 0xbe,
        "loader byte array does not contain classfile magic");
    return new LoaderState(loader.getId(), bytecode.getId(), bytes);
  }

  private static byte[] unbox(Object[] values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      require(values[i] instanceof Byte, "bytecode element " + i + " is not Byte");
      bytes[i] = (Byte) values[i];
    }
    return bytes;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(
          "[HPROF-JPF] class-loader state characterization failed: " + message);
    }
  }

  private static final class LoaderState {
    final long loaderId;
    final long byteArrayId;
    final byte[] classBytes;

    LoaderState(long loaderId, long byteArrayId, byte[] classBytes) {
      this.loaderId = loaderId;
      this.byteArrayId = byteArrayId;
      this.classBytes = classBytes;
    }
  }
}
