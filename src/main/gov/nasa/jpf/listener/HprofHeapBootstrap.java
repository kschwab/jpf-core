package gov.nasa.jpf.listener;

import com.squareup.haha.perflib.Snapshot;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.hprof.HprofGraphIdentityValidator;
import gov.nasa.jpf.vm.hprof.HprofPassASmokeGcVerifier;
import gov.nasa.jpf.vm.hprof.HprofPassASmokeRootBinder;
import gov.nasa.jpf.vm.hprof.HprofPassASmokeValidator;
import gov.nasa.jpf.vm.hprof.HprofSnapshotLoader;
import gov.nasa.jpf.vm.hprof.HprofTestRootBinder;
import gov.nasa.jpf.vm.hprof.HprofView;
import gov.nasa.jpf.vm.hprof.JpfHeapImporter;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Configures and launches host-side HPROF heap reconstruction during VM initialization. */
public class HprofHeapBootstrap extends ListenerAdapter {
  private final File hprof;
  private final Set<String> selectedClasses;
  private final boolean smokeBindRoot;
  private final boolean smokeValidate;
  private final boolean smokeGcValidate;
  private HprofPassASmokeGcVerifier smokeGcVerifier;
  private final String testRootSourceClass;
  private final String testRootHolderClass;
  private final String testRootField;
  private final boolean graphIdentityValidate;
  private HprofGraphIdentityValidator graphIdentityValidator;

  public HprofHeapBootstrap(Config conf) {
    String path = conf.getString("hprof.file");
    if (path == null || path.trim().isEmpty()) {
      throw new JPFConfigException("Missing required property: hprof.file");
    }
    hprof = new File(path);
    if (!hprof.isFile()) {
      throw new JPFConfigException("hprof.file not found: " + hprof.getAbsolutePath());
    }

    String[] classNames = conf.getCompactTrimmedStringArray("hprof.classes");
    if (classNames.length == 0) {
      throw new JPFConfigException("Missing required property: hprof.classes");
    }
    selectedClasses = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(classNames)));
    smokeBindRoot = conf.getBoolean("hprof.smoke_bind_root", false);
    smokeValidate = conf.getBoolean("hprof.smoke_validate", false);
    smokeGcValidate = conf.getBoolean("hprof.smoke_gc_validate", false);
    if (smokeGcValidate && !smokeBindRoot) {
      throw new JPFConfigException("hprof.smoke_gc_validate requires hprof.smoke_bind_root=true");
    }

    testRootSourceClass = conf.getString("hprof.test_root.source_class");
    testRootHolderClass = conf.getString("hprof.test_root.holder_class");
    testRootField = conf.getString("hprof.test_root.field");
    graphIdentityValidate = conf.getBoolean("hprof.graph_identity_validate", false);
    int testRootParts = nonEmpty(testRootSourceClass) + nonEmpty(testRootHolderClass)
        + nonEmpty(testRootField);
    if (testRootParts != 0 && testRootParts != 3) {
      throw new JPFConfigException("hprof.test_root requires source_class, holder_class, and field");
    }
    if (graphIdentityValidate && testRootParts != 3) {
      throw new JPFConfigException("hprof.graph_identity_validate requires hprof.test_root configuration");
    }
  }

  public HprofHeapBootstrap(Config conf, JPF jpf) {
    this(conf);
  }

  @Override
  public void vmInitialized(VM vm) {
    try {
      Snapshot snapshot = HprofSnapshotLoader.load(hprof);
      HprofView view = HprofView.from(snapshot);
      JpfHeapImporter.ImportResult result =
          new JpfHeapImporter().importHeap(vm, view, selectedClasses);
      if (smokeBindRoot) {
        int fooRef = HprofPassASmokeRootBinder.bind(vm, view, result);
        if (smokeGcValidate) {
          smokeGcVerifier = HprofPassASmokeGcVerifier.create(vm, result, fooRef);
        }
      }
      if (testRootSourceClass != null) {
        int graphRef = HprofTestRootBinder.bind(vm, view, result, testRootSourceClass,
            testRootHolderClass, testRootField);
        if (graphIdentityValidate) {
          graphIdentityValidator = HprofGraphIdentityValidator.create(vm, view, result, graphRef);
        }
      }
      if (smokeValidate) {
        HprofPassASmokeValidator.validate(vm, view, result);
      }
    } catch (Throwable t) {
      throw new RuntimeException("Failed to import selected HPROF heap into JPF", t);
    }
  }

  @Override
  public void gcBegin(VM vm) {
    if (smokeGcVerifier != null) {
      smokeGcVerifier.gcBegin();
    }
    if (graphIdentityValidator != null) {
      graphIdentityValidator.gcBegin();
    }
  }

  @Override
  public void gcEnd(VM vm) {
    if (smokeGcVerifier != null) {
      smokeGcVerifier.gcEnd(vm);
    }
    if (graphIdentityValidator != null) {
      graphIdentityValidator.gcEnd(vm);
    }
  }

  private static int nonEmpty(String value) {
    return value != null && !value.trim().isEmpty() ? 1 : 0;
  }
}
