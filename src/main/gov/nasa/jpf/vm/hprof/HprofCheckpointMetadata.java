package gov.nasa.jpf.vm.hprof;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Versioned supplemental checkpoint state that standard HPROF does not encode. */
public final class HprofCheckpointMetadata {
  public enum ClassLifecycle {
    UNINITIALIZED,
    INITIALIZED
  }

  private static final String VERSION_KEY = "checkpoint.version";
  private static final String CLASS_PREFIX = "class.";
  private static final HprofCheckpointMetadata EMPTY =
      new HprofCheckpointMetadata(Collections.emptyMap());

  private final Map<String, ClassLifecycle> classLifecycles;

  private HprofCheckpointMetadata(Map<String, ClassLifecycle> classLifecycles) {
    this.classLifecycles = Collections.unmodifiableMap(new LinkedHashMap<>(classLifecycles));
  }

  public static HprofCheckpointMetadata empty() {
    return EMPTY;
  }

  public static HprofCheckpointMetadata load(File file) throws IOException {
    if (file == null || !file.isFile()) {
      throw new IllegalArgumentException("lifecycle metadata file not found: " + file);
    }
    Map<String, String> entries = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), StandardCharsets.UTF_8))) {
      String line;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
          continue;
        }
        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
          throw malformed(file, lineNumber, "expected key=value");
        }
        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        if (key.isEmpty() || value.isEmpty()) {
          throw malformed(file, lineNumber, "empty key or value");
        }
        if (entries.put(key, value) != null) {
          throw malformed(file, lineNumber, "duplicate key: " + key);
        }
      }
    }

    String version = entries.remove(VERSION_KEY);
    if (version == null) {
      throw new IllegalArgumentException("missing " + VERSION_KEY + " in " + file);
    }
    if (!"1".equals(version)) {
      throw new IllegalArgumentException(
          "unsupported checkpoint metadata version " + version + " in " + file);
    }

    Map<String, ClassLifecycle> lifecycles = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : entries.entrySet()) {
      String key = entry.getKey();
      if (!key.startsWith(CLASS_PREFIX) || key.length() == CLASS_PREFIX.length()) {
        throw new IllegalArgumentException("unknown checkpoint metadata key " + key + " in " + file);
      }
      String className = key.substring(CLASS_PREFIX.length());
      ClassLifecycle lifecycle;
      try {
        lifecycle = ClassLifecycle.valueOf(entry.getValue());
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("unknown lifecycle " + entry.getValue()
            + " for " + className + " in " + file, ex);
      }
      lifecycles.put(className, lifecycle);
    }
    return new HprofCheckpointMetadata(lifecycles);
  }

  public ClassLifecycle getClassLifecycle(String binaryClassName) {
    return classLifecycles.get(binaryClassName);
  }

  public Set<String> getClassNames() {
    return classLifecycles.keySet();
  }

  public boolean isEmpty() {
    return classLifecycles.isEmpty();
  }

  private static IllegalArgumentException malformed(File file, int line, String message) {
    return new IllegalArgumentException(
        "malformed checkpoint metadata " + file + " at line " + line + ": " + message);
  }
}
