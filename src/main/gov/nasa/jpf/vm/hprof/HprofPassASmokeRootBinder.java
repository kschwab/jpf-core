package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.vm.VM;

/** Frozen Foo/Bar smoke wrapper around the parameterized test-only root binder. */
public final class HprofPassASmokeRootBinder {
  private HprofPassASmokeRootBinder() {}

  public static int bind(VM vm, HprofView view, JpfHeapImporter.ImportResult result) {
    return HprofTestRootBinder.bind(vm, view, result,
        "HprofPassASmokeGraph$Foo", "HprofPassASmokeGraph", "root");
  }
}
