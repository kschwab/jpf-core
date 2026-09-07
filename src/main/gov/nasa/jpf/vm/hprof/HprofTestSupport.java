package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.vm.VM;

/** Optional fixture-specific verification around the generic HPROF importer. */
public interface HprofTestSupport {
  void initialize(VM vm, HprofView view, JpfHeapImporter.ImportResult result, int rootRef);
  default void gcBegin() {}
  default void gcEnd(VM vm) {}
}
