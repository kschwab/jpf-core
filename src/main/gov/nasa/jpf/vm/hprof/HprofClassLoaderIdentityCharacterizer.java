package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.Type;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Characterizes loader-qualified class identity preserved by HPROF and HAHA. */
public final class HprofClassLoaderIdentityCharacterizer {
  private static final String BINARY_NAME = "hprof.loader.Duplicate";
  private static final String HAHA_NAME = "hprof/loader/Duplicate";

  private HprofClassLoaderIdentityCharacterizer() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException(
          "usage: HprofClassLoaderIdentityCharacterizer <same-name-loaders.hprof>");
    }
    Snapshot snapshot = HprofSnapshotLoader.load(new File(args[0]));
    HprofView view = HprofView.from(snapshot);

    List<ClassObj> duplicates = new ArrayList<>(snapshot.findClasses(HAHA_NAME));
    duplicates.sort(Comparator.comparingLong(ClassObj::getId));
    require(duplicates.size() == 2,
        "expected two same-named ClassObj records, found " + duplicates.size());
    require(snapshot.findClass(HAHA_NAME) == null,
        "singular name lookup should report ambiguity for duplicate definitions");

    Definition first = inspect(duplicates.get(0), view);
    Definition second = inspect(duplicates.get(1), view);
    require(first.classId != second.classId, "same-named ClassObj IDs collided");
    require(first.loaderId != second.loaderId, "defining loader IDs collided");
    require(first.instanceId != second.instanceId, "fixture instance IDs collided");
    require((first.marker == 111 && second.marker == 222)
            || (first.marker == 222 && second.marker == 111),
        "expected marker values 111 and 222");
    require(view.classes.get(first.classId) == first.classObj
            && view.classes.get(second.classId) == second.classObj,
        "HprofView did not retain both ClassObj identities by HPROF ID");
    require(view.instances.get(first.instanceId) == first.instance
            && view.instances.get(second.instanceId) == second.instance,
        "HprofView did not retain both instance identities by HPROF ID");

    print(first);
    print(second);
    System.out.println("[HPROF-JPF] HAHA same-name lookup: findClasses=2 findClass=null(ambiguous)");
    System.out.println("[HPROF-JPF] HprofView loader identity: class-id index preserved both; no name index collision");
    System.out.println("[HPROF-JPF] instance/class/loader association verified: markers={111,222}");
    System.out.println("[HPROF-JPF] loader-qualified identity characterization result: CASE_A");
  }

  private static Definition inspect(ClassObj classObj, HprofView view) {
    Instance loader = classObj.getClassLoader();
    require(loader != null, "custom-loaded " + BINARY_NAME + " has null/bootstrap loader");
    List<Instance> exactInstances = new ArrayList<>();
    for (Instance candidate : view.instances.values()) {
      if (candidate.getClassObj() == classObj) {
        exactInstances.add(candidate);
      }
    }
    require(exactInstances.size() == 1,
        "expected one identity-associated instance for ClassObj 0x"
            + Long.toHexString(classObj.getId()) + ", found " + exactInstances.size());
    Instance instance = exactInstances.get(0);
    require(instance instanceof ClassInstance, "duplicate instance is not ClassInstance");
    require(instance.getClassObj() == classObj,
        "instance did not resolve to its exact loader-qualified ClassObj");

    Integer marker = null;
    for (ClassInstance.FieldValue fieldValue : ((ClassInstance) instance).getValues()) {
      Field field = fieldValue.getField();
      if ("marker".equals(field.getName())) {
        require(field.getType() == Type.INT && fieldValue.getValue() instanceof Integer,
            "marker has unexpected HAHA representation");
        marker = (Integer) fieldValue.getValue();
      }
    }
    require(marker != null, "missing marker field");
    return new Definition(classObj, loader, instance, marker);
  }

  private static void print(Definition definition) {
    System.out.printf("[HPROF-JPF] loader class=%s classHPROF=0x%x loaderHPROF=0x%x "
            + "loaderClass=%s instanceHPROF=0x%x marker=%d%n",
        definition.classObj.getClassName(), definition.classId, definition.loaderId,
        definition.loader.getClassObj().getClassName(), definition.instanceId, definition.marker);
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(
          "[HPROF-JPF] class-loader identity characterization failed: " + message);
    }
  }

  private static final class Definition {
    final ClassObj classObj;
    final Instance loader;
    final Instance instance;
    final long classId;
    final long loaderId;
    final long instanceId;
    final int marker;

    Definition(ClassObj classObj, Instance loader, Instance instance, int marker) {
      this.classObj = classObj;
      this.loader = loader;
      this.instance = instance;
      this.classId = classObj.getId();
      this.loaderId = loader.getId();
      this.instanceId = instance.getId();
      this.marker = marker;
    }
  }
}
