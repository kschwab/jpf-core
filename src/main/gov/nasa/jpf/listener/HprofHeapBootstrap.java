package gov.nasa.jpf.listener;

import com.squareup.haha.perflib.Snapshot;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.hprof.HprofCheckpointMetadata;
import gov.nasa.jpf.vm.hprof.HprofCheckpointBundle;
import gov.nasa.jpf.vm.hprof.HprofLoaderImportContext;
import gov.nasa.jpf.vm.hprof.HprofLoaderQualifiedValidator;
import gov.nasa.jpf.vm.hprof.HprofGraphIdentityValidator;
import gov.nasa.jpf.vm.hprof.HprofPassASmokeGcVerifier;
import gov.nasa.jpf.vm.hprof.HprofPassASmokeRootBinder;
import gov.nasa.jpf.vm.hprof.HprofPassASmokeValidator;
import gov.nasa.jpf.vm.hprof.HprofSnapshotLoader;
import gov.nasa.jpf.vm.hprof.HprofTestRootBinder;
import gov.nasa.jpf.vm.hprof.HprofTestSupport;
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
  private final HprofCheckpointBundle checkpointBundle;
  private final boolean loaderQualifiedValidate;
  private final Set<String> selectedClasses;
  private final Set<String> selectedStaticClasses;
  private final HprofCheckpointMetadata checkpointMetadata;
  private final boolean smokeBindRoot;
  private final boolean smokeValidate;
  private final boolean smokeGcValidate;
  private HprofPassASmokeGcVerifier smokeGcVerifier;
  private final String testRootSourceClass;
  private final String testRootHolderClass;
  private final String testRootField;
  private final boolean graphIdentityValidate;
  private HprofGraphIdentityValidator graphIdentityValidator;
  private final HprofTestSupport testSupport;

  public HprofHeapBootstrap(Config conf) {
    String path = conf.getString("hprof.file");
    String bundlePath = conf.getString("hprof.bundle");
    HprofCheckpointBundle loadedBundle = null;
    if (bundlePath != null && !bundlePath.trim().isEmpty()) {
      try {
        loadedBundle = HprofCheckpointBundle.loadAndVerify(new File(bundlePath));
      } catch (Exception ex) {
        throw new JPFConfigException("invalid hprof.bundle: " + bundlePath, ex);
      }
    }
    checkpointBundle = loadedBundle;
    if (checkpointBundle != null) {
      hprof = checkpointBundle.hprof.toFile();
      if (path != null && !path.trim().isEmpty()) {
        try {
          if (!new File(path).getCanonicalFile().equals(hprof.getCanonicalFile())) {
            throw new JPFConfigException(
                "hprof.file does not match the HPROF bound by hprof.bundle");
          }
        } catch (java.io.IOException ex) {
          throw new JPFConfigException("cannot compare hprof.file and hprof.bundle", ex);
        }
      }
    } else {
      if (path == null || path.trim().isEmpty()) {
        throw new JPFConfigException("Missing required property: hprof.file");
      }
      hprof = new File(path);
    }
    if (!hprof.isFile()) {
      throw new JPFConfigException("hprof.file not found: " + hprof.getAbsolutePath());
    }
    loaderQualifiedValidate = conf.getBoolean("hprof.loader_qualified_validate", false);

    String[] classNames = conf.getCompactTrimmedStringArray("hprof.classes");
    selectedClasses = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(classNames)));
    String[] staticClassNames = conf.getCompactTrimmedStringArray("hprof.static_classes");
    selectedStaticClasses = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(staticClassNames)));
    if (selectedClasses.isEmpty() && selectedStaticClasses.isEmpty()
        && checkpointBundle == null) {
      throw new JPFConfigException(
          "At least one of hprof.classes or hprof.static_classes is required");
    }
    String lifecyclePath = conf.getString("hprof.lifecycle_file");
    if (lifecyclePath == null || lifecyclePath.trim().isEmpty()) {
      checkpointMetadata = HprofCheckpointMetadata.empty();
    } else {
      File lifecycleFile = new File(lifecyclePath);
      if (!lifecycleFile.isFile()) {
        throw new JPFConfigException(
            "hprof.lifecycle_file not found: " + lifecycleFile.getAbsolutePath());
      }
      try {
        checkpointMetadata = HprofCheckpointMetadata.load(lifecycleFile);
      } catch (Exception ex) {
        throw new JPFConfigException(
            "invalid hprof.lifecycle_file: " + lifecycleFile.getAbsolutePath(), ex);
      }
    }
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
    testSupport = conf.getInstance("hprof.test_support.class", HprofTestSupport.class);
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
      HprofLoaderImportContext loaderContext = checkpointBundle == null
          ? null : HprofLoaderImportContext.create(vm, view, checkpointBundle);
      JpfHeapImporter.ImportResult result =
          new JpfHeapImporter().importHeap(
              vm, view, selectedClasses, selectedStaticClasses, checkpointMetadata, loaderContext);
      if (loaderQualifiedValidate) {
        if (checkpointBundle == null) {
          throw new IllegalStateException(
              "hprof.loader_qualified_validate requires hprof.bundle");
        }
        HprofLoaderQualifiedValidator.validateAndBind(
            vm, view, checkpointBundle, loaderContext, result);
      }
      if (smokeBindRoot) {
        int fooRef = HprofPassASmokeRootBinder.bind(vm, view, result);
        if (smokeGcValidate) {
          smokeGcVerifier = HprofPassASmokeGcVerifier.create(vm, result, fooRef);
        }
      }
      int testRootRef = MJIEnv.NULL;
      if (testRootSourceClass != null) {
        testRootRef = HprofTestRootBinder.bind(vm, view, result, testRootSourceClass,
            testRootHolderClass, testRootField);
        if (graphIdentityValidate) {
          graphIdentityValidator = HprofGraphIdentityValidator.create(
              vm, view, result, testRootRef);
        }
      }
      if (testSupport != null) {
        testSupport.initialize(vm, view, result, testRootRef);
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
    if (testSupport != null) {
      testSupport.gcBegin();
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
    if (testSupport != null) {
      testSupport.gcEnd(vm);
    }
  }

  private static int nonEmpty(String value) {
    return value != null && !value.trim().isEmpty() ? 1 : 0;
  }
}
