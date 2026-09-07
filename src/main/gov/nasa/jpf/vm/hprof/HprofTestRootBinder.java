package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.Instance;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import java.util.Map;

/** Test-only bridge from one exact imported HPROF class to modeled static storage. */
public final class HprofTestRootBinder {
  private HprofTestRootBinder() {}

  public static int bind(VM vm, HprofView view, JpfHeapImporter.ImportResult result,
      String sourceClassName, String holderClassName, String staticFieldName) {
    ClassInstance source = findExactlyOne(view, sourceClassName);
    Map<Long, Integer> refMap = result.getRefMap();
    Integer sourceRef = refMap.get(source.getId());
    require(sourceRef != null && sourceRef != MJIEnv.NULL,
        "imported source has no non-null JPF mapping: " + sourceClassName);
    ElementInfo sourceEi = vm.getHeap().get(sourceRef);
    require(sourceEi != null && sourceClassName.equals(sourceEi.getClassInfo().getName()),
        "mapped source reference has the wrong JPF ClassInfo");

    ThreadInfo ti = vm.getCurrentThread();
    require(ti != null, "no current JPF thread while binding test root");
    ClassInfo holderClass = ClassLoaderInfo.getSystemResolvedClassInfo(holderClassName);
    if (!holderClass.isRegistered()) {
      holderClass.registerClass(ti);
    }
    boolean pushedClinit = holderClass.initializeClass(ti);
    require(!pushedClinit,
        holderClassName + " unexpectedly requires <clinit>; binding would not be stable");
    require(holderClass.isInitialized(), holderClassName + " was not initialized before root binding");

    StaticElementInfo staticEi = holderClass.getModifiableStaticElementInfo();
    require(staticEi != null, "no static storage exists for " + holderClassName);
    staticEi.setReferenceField(staticFieldName, sourceRef);
    require(staticEi.getReferenceField(staticFieldName) == sourceRef,
        "modeled test root did not retain the imported reference");

    System.out.println("[HPROF-JPF] smoke root bound: " + holderClassName + "."
        + staticFieldName + " -> JPF ref " + sourceRef);
    return sourceRef;
  }

  private static ClassInstance findExactlyOne(HprofView view, String className) {
    ClassInstance found = null;
    for (Instance instance : view.instances.values()) {
      if (instance.getClassObj() != null && className.equals(instance.getClassObj().getClassName())) {
        require(instance instanceof ClassInstance, className + " is not a ClassInstance");
        require(found == null, "multiple HPROF instances found for " + className);
        found = (ClassInstance) instance;
      }
    }
    require(found != null, "no HPROF instance found for " + className);
    return found;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] test root binding failed: " + message);
    }
  }
}
