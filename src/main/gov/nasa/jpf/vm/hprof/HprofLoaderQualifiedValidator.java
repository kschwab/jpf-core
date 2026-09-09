package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** H.4 host-side identity checks and test-only modeled root binding. */
public final class HprofLoaderQualifiedValidator {
  private HprofLoaderQualifiedValidator() {}

  public static void validateAndBind(VM vm, HprofView view, HprofCheckpointBundle bundle,
      HprofLoaderImportContext context, JpfHeapImporter.ImportResult result) {
    require(context != null, "loader import context is missing");
    require(context.getLoaderMap().size() == 2, "expected two modeled loader mappings");
    require(context.getClassMap().size() == 2, "expected two modeled class mappings");
    require(result.getAllocatedObjects() == 2 && result.getRefMap().size() == 2,
        "expected exactly two imported captured instances");

    List<HprofCheckpointBundle.ClassDefinition> definitions = new ArrayList<>(bundle.classes);
    definitions.sort(Comparator.comparingLong(value -> value.logicalId));
    int[] refs = new int[2];
    int[] markers = {1111, 2222};
    ClassInfo[] classes = new ClassInfo[2];
    ClassLoaderInfo system = ClassLoaderInfo.getCurrentSystemClassLoader();

    for (int i = 0; i < definitions.size(); i++) {
      HprofCheckpointBundle.ClassDefinition definition = definitions.get(i);
      ClassObj sourceClass = view.classes.get(definition.hprofClassId);
      ClassInstance sourceInstance = exactlyOneInstance(view, sourceClass);
      Integer ref = result.getRefMap().get(sourceInstance.getId());
      require(ref != null, "captured instance has no JPF reference mapping");
      ElementInfo ei = vm.getHeap().get(ref);
      require(ei != null, "mapped imported instance is absent");
      ClassInfo ci = context.getClassInfo(definition.hprofClassId);
      require(ei.getClassInfo() == ci, "imported instance has wrong exact ClassInfo");
      require(ci.getClassLoaderInfo() != system, "system-loader ClassInfo substituted");
      require(ei.getIntField("marker") == markers[i], "captured marker was not restored");
      refs[i] = ref;
      classes[i] = ci;
      HprofCheckpointBundle.LoaderDefinition loader =
          bundle.loaders.get(definition.logicalLoaderId);
      System.out.printf("[HPROF-JPF] loader-qualified class %s: hprofLoader=0x%x "
              + "jpfLoader=%d classObj=0x%x classUniqueId=0x%x objectRef=%d marker=%d%n",
          i == 0 ? "A" : "B", loader.hprofLoaderId, ci.getClassLoaderInfo().getId(),
          definition.hprofClassId, ci.getUniqueId(), ref, markers[i]);
    }

    require(classes[0] != classes[1], "same ClassInfo used for both definitions");
    require(classes[0].getName().equals(classes[1].getName()), "class names differ");
    require(classes[0].getClassLoaderInfo() != classes[1].getClassLoaderInfo(),
        "same ClassLoaderInfo used for both definitions");
    require(classes[0].getUniqueId() != classes[1].getUniqueId(), "ClassInfo IDs collided");

    bindRoots(vm, refs[0], refs[1]);
    System.out.println("[HPROF-JPF] loader-qualified import verified: "
        + "loaders=2 bundledClasses=2 objects=2 mappings=2 systemFallback=false");
  }

  private static ClassInstance exactlyOneInstance(HprofView view, ClassObj classObj) {
    ClassInstance found = null;
    for (Instance instance : view.instances.values()) {
      if (instance.getClassObj() == classObj) {
        require(instance instanceof ClassInstance, "selected instance is not ClassInstance");
        require(found == null, "multiple instances found for bundled ClassObj");
        found = (ClassInstance) instance;
      }
    }
    require(found != null, "no captured instance found for bundled ClassObj");
    return found;
  }

  private static void bindRoots(VM vm, int first, int second) {
    ThreadInfo ti = vm.getCurrentThread();
    ClassInfo holder = ClassLoaderInfo.getSystemResolvedClassInfo("HprofLoaderQualifiedRoots");
    if (!holder.isRegistered()) holder.registerClass(ti);
    boolean pushed = holder.initializeClass(ti);
    require(!pushed && holder.isInitialized(), "test root holder initialization was not immediate");
    StaticElementInfo statics = holder.getModifiableStaticElementInfo();
    statics.setReferenceField("first", first);
    statics.setReferenceField("second", second);
    require(statics.getReferenceField("first") == first
        && statics.getReferenceField("second") == second, "modeled roots did not retain refs");
    System.out.printf("[HPROF-JPF] loader-qualified test roots bound: first=%d second=%d%n",
        first, second);
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] loader-qualified validation failed: " + message);
    }
  }
}
