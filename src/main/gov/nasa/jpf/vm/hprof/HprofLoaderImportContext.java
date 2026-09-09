package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ClassPath;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bundle-local mappings from captured HPROF loader/class identities to modeled JPF identities.
 *
 * <p>H.4 deliberately supports only parentless captured loaders and definitions explicitly
 * integrity-bound by {@link HprofCheckpointBundle}.
 */
public final class HprofLoaderImportContext {
  private final Map<Long, ClassLoaderInfo> loaderMap;
  private final Map<Long, ClassInfo> classMap;

  private HprofLoaderImportContext(
      Map<Long, ClassLoaderInfo> loaderMap, Map<Long, ClassInfo> classMap) {
    this.loaderMap = Collections.unmodifiableMap(loaderMap);
    this.classMap = Collections.unmodifiableMap(classMap);
  }

  public static HprofLoaderImportContext create(
      VM vm, HprofView view, HprofCheckpointBundle bundle) throws IOException {
    ThreadInfo ti = vm.getCurrentThread();
    require(ti != null, "no current JPF thread while reconstructing loaders");

    Map<Long, ClassLoaderInfo> loaders = new LinkedHashMap<>();
    for (HprofCheckpointBundle.LoaderDefinition definition : bundle.loaders.values()) {
      Instance source = view.instances.get(definition.hprofLoaderId);
      require(source instanceof ClassInstance,
          "bundle loader HPROF ID is not a ClassInstance: 0x"
              + Long.toHexString(definition.hprofLoaderId));
      Object parent = fieldValue((ClassInstance) source, "parent");
      require(parent == null,
          "non-null parent loader is unsupported for HPROF loader 0x"
              + Long.toHexString(definition.hprofLoaderId));

      ClassInfo modeledLoaderClass =
          ClassLoaderInfo.getSystemResolvedClassInfo("java.lang.ClassLoader");
      ElementInfo modeledLoader = vm.getHeap().newObject(modeledLoaderClass, ti);
      require(modeledLoader.getObjectRef() != MJIEnv.NULL,
          "modeled ClassLoader allocation returned NULL");
      ClassLoaderInfo cli =
          new CheckpointClassLoaderInfo(vm, modeledLoader.getObjectRef(), new ClassPath(),
              ClassLoaderInfo.getCurrentSystemClassLoader());
      require(loaders.put(definition.hprofLoaderId, cli) == null,
          "duplicate captured loader HPROF ID 0x"
              + Long.toHexString(definition.hprofLoaderId));
      System.out.printf("[HPROF-JPF] loader mapping HPROF=0x%x JPF-loader=%d objectRef=%d "
              + "capturedParent=null modeledParent=system%n",
          definition.hprofLoaderId, cli.getId(), modeledLoader.getObjectRef());
    }

    Map<Long, ClassInfo> classes = new LinkedHashMap<>();
    for (HprofCheckpointBundle.ClassDefinition definition : bundle.classes) {
      ClassObj sourceClass = view.classes.get(definition.hprofClassId);
      require(sourceClass != null,
          "bundle ClassObj not found: 0x" + Long.toHexString(definition.hprofClassId));
      Instance sourceLoader = sourceClass.getClassLoader();
      require(sourceLoader != null,
          "bundled custom class has null defining loader: " + definition.binaryName);
      HprofCheckpointBundle.LoaderDefinition loaderDefinition =
          bundle.loaders.get(definition.logicalLoaderId);
      require(loaderDefinition != null,
          "class references unknown logical loader " + definition.logicalLoaderId);
      require(sourceLoader.getId() == loaderDefinition.hprofLoaderId,
          "ClassObj loader does not match bundle loader for " + definition.binaryName);
      require(normalizeHprofName(sourceClass.getClassName()).equals(definition.binaryName),
          "ClassObj name does not match bundled binary name: " + sourceClass.getClassName());

      ClassLoaderInfo cli = loaders.get(loaderDefinition.hprofLoaderId);
      require(cli != null, "no modeled loader mapping for " + definition.binaryName);
      byte[] bytes = Files.readAllBytes(definition.artifact);
      ClassInfo ci = cli.getResolvedClassInfo(definition.binaryName, bytes, 0, bytes.length);
      require(ci.getClassLoaderInfo() == cli,
          "bundled class resolved under the wrong modeled loader: " + definition.binaryName);
      require(definition.binaryName.equals(ci.getName()),
          "bundled class bytes define the wrong name: expected=" + definition.binaryName
              + " actual=" + ci.getName());
      if (!ci.isRegistered()) {
        ci.registerClass(ti);
      }
      require(classes.put(definition.hprofClassId, ci) == null,
          "duplicate bundled ClassObj HPROF ID 0x"
              + Long.toHexString(definition.hprofClassId));
      System.out.printf("[HPROF-JPF] class mapping ClassObj=0x%x class=%s "
              + "JPF-loader=%d classUniqueId=0x%x%n",
          definition.hprofClassId, ci.getName(), cli.getId(), ci.getUniqueId());
    }

    return new HprofLoaderImportContext(loaders, classes);
  }

  public Map<Long, ClassLoaderInfo> getLoaderMap() {
    return loaderMap;
  }

  public Map<Long, ClassInfo> getClassMap() {
    return classMap;
  }

  public ClassInfo getClassInfo(long hprofClassId) {
    return classMap.get(hprofClassId);
  }

  private static Object fieldValue(ClassInstance instance, String name) {
    for (ClassInstance.FieldValue value : instance.getValues()) {
      if (name.equals(value.getField().getName())) {
        return value.getValue();
      }
    }
    throw new IllegalStateException(
        "[HPROF-JPF] captured loader has no inherited field named " + name);
  }

  private static String normalizeHprofName(String name) {
    return name.replace('/', '.');
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] loader import failed: " + message);
    }
  }

  private static final class CheckpointClassLoaderInfo extends ClassLoaderInfo {
    @Override
    protected boolean isRoundTripRequired() {
      return false;
    }

    CheckpointClassLoaderInfo(VM vm, int objectRef, ClassPath classPath, ClassLoaderInfo parent) {
      super(vm, objectRef, classPath, parent);
    }
  }
}
