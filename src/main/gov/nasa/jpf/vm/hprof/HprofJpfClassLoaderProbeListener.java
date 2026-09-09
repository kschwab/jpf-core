package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.VM;
import java.util.ArrayList;
import java.util.List;

/** Host-side assertions for the modeled two-loader defineClass probe. */
public final class HprofJpfClassLoaderProbeListener extends ListenerAdapter {
  private static final String DUPLICATE = "hprof.loader.Duplicate";
  private final List<ClassInfo> definitions = new ArrayList<>();

  @Override
  public void classLoaded(VM vm, ClassInfo loadedClass) {
    if (DUPLICATE.equals(loadedClass.getName())
        && !loadedClass.getClassLoaderInfo().isSystemClassLoader()) {
      definitions.add(loadedClass);
      System.out.printf("[HPROF-JPF] JPF loader probe class=%s loaderId=%d "
              + "loaderObjectRef=%d classUniqueId=0x%x%n",
          loadedClass.getName(), loadedClass.getClassLoaderInfo().getId(),
          loadedClass.getClassLoaderInfo().getClassLoaderObjectRef(), loadedClass.getUniqueId());
    }
  }

  @Override
  public void searchFinished(Search search) {
    require(definitions.size() == 2,
        "expected two loader-qualified ClassInfo definitions, found " + definitions.size());
    ClassInfo first = definitions.get(0);
    ClassInfo second = definitions.get(1);
    require(first != second, "same ClassInfo object used for both loaders");
    require(first.getName().equals(second.getName()), "ClassInfo names differ");
    require(first.getClassLoaderInfo() != second.getClassLoaderInfo(),
        "ClassInfo defining loaders are identical");
    require(first.getClassLoaderInfo().getClassLoaderObjectRef()
            != second.getClassLoaderInfo().getClassLoaderObjectRef(),
        "modeled loader object references are identical");
    require(first.getUniqueId() != second.getUniqueId(), "ClassInfo unique IDs collided");
    System.out.println("[HPROF-JPF] JPF minimum loader state verified: "
        + "modeledObjects=2 ClassLoaderInfos=2 ClassInfos=2 uniqueIds=2");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("[HPROF-JPF] JPF loader probe failed: " + message);
    }
  }
}
