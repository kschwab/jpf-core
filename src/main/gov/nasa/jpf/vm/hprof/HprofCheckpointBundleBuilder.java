package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** H.3 postprocessor resolving heap anchors and finalizing a bundle manifest. */
public final class HprofCheckpointBundleBuilder {
  private static final String LOADER_TAG = "HprofCheckpointBundleCapture$LoaderTag";
  private static final String CLASS_TAG = "HprofCheckpointBundleCapture$ClassTag";
  private static final String CLASS_NAME = "hprof.loader.Duplicate";
  private static final String HAHA_CLASS_NAME = "hprof/loader/Duplicate";

  private HprofCheckpointBundleBuilder() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofCheckpointBundleBuilder <bundle-dir>");
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Path hprof = root.resolve("heap.hprof");
    Snapshot snapshot = HprofSnapshotLoader.load(hprof.toFile());
    HprofView view = HprofView.from(snapshot);
    Map<Long, Instance> loaders = loaderAnchors(view);
    Map<Long, ClassAnchor> classes = classAnchors(view);
    require(loaders.size() == 2 && classes.size() == 2, "expected two loader and class anchors");

    StringBuilder manifest = new StringBuilder();
    manifest.append("bundle.version=1\n");
    manifest.append("hprof.file=heap.hprof\n");
    manifest.append("hprof.sha256=").append(HprofCheckpointBundle.sha256(hprof)).append('\n');
    manifest.append("loader.count=2\n");
    for (long id = 1; id <= 2; id++) {
      Instance loader = required(loaders, id, "loader");
      manifest.append("loader.").append(id).append(".hprof_id=0x")
          .append(Long.toHexString(loader.getId())).append('\n');
    }
    manifest.append("class.count=2\n");
    for (long id = 1; id <= 2; id++) {
      ClassAnchor anchor = required(classes, id, "class");
      Instance loader = required(loaders, anchor.logicalLoaderId, "class loader");
      require(anchor.loader == loader, "class tag loader does not match loader tag " + id);
      ClassObj classObj = findClass(snapshot, loader);
      Path artifact = root.resolve("classes/class-" + id + ".class");
      manifest.append("class.").append(id).append(".loader=")
          .append(anchor.logicalLoaderId).append('\n');
      manifest.append("class.").append(id).append(".name=").append(CLASS_NAME).append('\n');
      manifest.append("class.").append(id).append(".hprof_id=0x")
          .append(Long.toHexString(classObj.getId())).append('\n');
      manifest.append("class.").append(id).append(".file=classes/class-")
          .append(id).append(".class\n");
      manifest.append("class.").append(id).append(".sha256=")
          .append(HprofCheckpointBundle.sha256(artifact)).append('\n');
      System.out.printf("[HPROF-JPF] bundle correlation class=%d loader=%d "
              + "loaderHPROF=0x%x classHPROF=0x%x%n",
          id, anchor.logicalLoaderId, loader.getId(), classObj.getId());
    }
    Files.write(root.resolve("bundle.properties"),
        manifest.toString().getBytes(StandardCharsets.UTF_8));

    HprofCheckpointBundle bundle = HprofCheckpointBundle.loadAndVerify(root.toFile());
    verifySemantics(snapshot, loaders, bundle);
    require(!java.util.Arrays.equals(Files.readAllBytes(bundle.classes.get(0).artifact),
        Files.readAllBytes(bundle.classes.get(1).artifact)), "class artifacts are identical");
    require(!bundle.classes.get(0).sha256.equals(bundle.classes.get(1).sha256),
        "class artifact hashes are identical");
    negativeIntegrityControl(root, bundle.classes.get(0));
    System.out.println("[HPROF-JPF] checkpoint bundle verified: hprofHash=true classHashes=true relationships=true");
    System.out.println("[HPROF-JPF] same-name/different-bytecode artifacts verified: hashesDistinct=true");
    System.out.println("[HPROF-JPF] negative integrity control verified: tampered artifact rejected");
  }

  private static Map<Long, Instance> loaderAnchors(HprofView view) {
    Map<Long, Instance> result = new LinkedHashMap<>();
    for (Instance raw : view.instances.values()) {
      if (raw instanceof ClassInstance && LOADER_TAG.equals(raw.getClassObj().getClassName())) {
        ClassInstance tag = (ClassInstance) raw;
        long id = longField(tag, "logicalLoaderId");
        Instance loader = instanceField(tag, "loader");
        require(result.put(id, loader) == null, "duplicate logical loader " + id);
      }
    }
    return result;
  }

  private static Map<Long, ClassAnchor> classAnchors(HprofView view) {
    Map<Long, ClassAnchor> result = new LinkedHashMap<>();
    for (Instance raw : view.instances.values()) {
      if (raw instanceof ClassInstance && CLASS_TAG.equals(raw.getClassObj().getClassName())) {
        ClassInstance tag = (ClassInstance) raw;
        long id = longField(tag, "logicalClassId");
        ClassAnchor anchor = new ClassAnchor(longField(tag, "logicalLoaderId"),
            instanceField(tag, "loader"));
        require(result.put(id, anchor) == null, "duplicate logical class " + id);
      }
    }
    return result;
  }

  private static ClassObj findClass(Snapshot snapshot, Instance loader) {
    ClassObj found = null;
    for (ClassObj candidate : snapshot.findClasses(HAHA_CLASS_NAME)) {
      if (candidate.getClassLoader() == loader) {
        require(found == null, "multiple class definitions for one loader and name");
        found = candidate;
      }
    }
    require(found != null, "no ClassObj for loader 0x" + Long.toHexString(loader.getId()));
    return found;
  }

  private static void verifySemantics(Snapshot snapshot, Map<Long, Instance> loaders,
      HprofCheckpointBundle bundle) {
    for (HprofCheckpointBundle.ClassDefinition definition : bundle.classes) {
      Instance loader = required(loaders, definition.logicalLoaderId, "manifest loader");
      ClassObj classObj = snapshot.findClass(definition.hprofClassId);
      require(classObj != null && classObj.getClassLoader() == loader,
          "manifest ClassObj/loader relationship mismatch for class " + definition.logicalId);
      require(HAHA_CLASS_NAME.equals(classObj.getClassName()), "manifest ClassObj name mismatch");
    }
  }

  private static void negativeIntegrityControl(Path root,
      HprofCheckpointBundle.ClassDefinition definition) throws Exception {
    Path tampered = root.resolve("classes/.tampered.class");
    Files.copy(definition.artifact, tampered, StandardCopyOption.REPLACE_EXISTING);
    byte[] bytes = Files.readAllBytes(tampered);
    bytes[bytes.length - 1] ^= 1;
    Files.write(tampered, bytes);
    try {
      HprofCheckpointBundle.requireSha256(tampered, definition.sha256, "tampered artifact");
      throw new IllegalStateException("tampered artifact was accepted");
    } catch (IllegalArgumentException expected) {
      require(expected.getMessage().contains("SHA-256 mismatch"),
          "tamper rejection had unclear diagnostic");
    } finally {
      Files.deleteIfExists(tampered);
    }
  }

  private static long longField(ClassInstance instance, String name) {
    Object value = field(instance, name);
    require(value instanceof Long, name + " is not Long");
    return (Long) value;
  }

  private static Instance instanceField(ClassInstance instance, String name) {
    Object value = field(instance, name);
    require(value instanceof Instance, name + " is not Instance");
    return (Instance) value;
  }

  private static Object field(ClassInstance instance, String name) {
    for (ClassInstance.FieldValue value : instance.getValues()) {
      if (name.equals(value.getField().getName())) return value.getValue();
    }
    throw new IllegalStateException("missing anchor field " + name);
  }

  private static <T> T required(Map<Long, T> values, long id, String description) {
    T value = values.get(id);
    require(value != null, "missing logical " + description + " " + id);
    return value;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException("[HPROF-JPF] bundle build failed: " + message);
  }

  private static final class ClassAnchor {
    final long logicalLoaderId;
    final Instance loader;

    ClassAnchor(long logicalLoaderId, Instance loader) {
      this.logicalLoaderId = logicalLoaderId;
      this.loader = loader;
    }
  }
}
