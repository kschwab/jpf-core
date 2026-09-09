package gov.nasa.jpf.vm.hprof;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Minimal version-one manifest model and integrity validator for an H.3 bundle directory. */
public final class HprofCheckpointBundle {
  public static final class ClassDefinition {
    public final long logicalId;
    public final long logicalLoaderId;
    public final String binaryName;
    public final long hprofClassId;
    public final Path artifact;
    public final String sha256;

    private ClassDefinition(long logicalId, long logicalLoaderId, String binaryName,
        long hprofClassId, Path artifact, String sha256) {
      this.logicalId = logicalId;
      this.logicalLoaderId = logicalLoaderId;
      this.binaryName = binaryName;
      this.hprofClassId = hprofClassId;
      this.artifact = artifact;
      this.sha256 = sha256;
    }
  }

  public final Path root;
  public final Path hprof;
  public final String hprofSha256;
  public final List<ClassDefinition> classes;

  private HprofCheckpointBundle(Path root, Path hprof, String hprofSha256,
      List<ClassDefinition> classes) {
    this.root = root;
    this.hprof = hprof;
    this.hprofSha256 = hprofSha256;
    this.classes = Collections.unmodifiableList(classes);
  }

  public static HprofCheckpointBundle loadAndVerify(File directory) throws IOException {
    Path root = directory.toPath().toAbsolutePath().normalize();
    Path manifest = root.resolve("bundle.properties");
    Properties values = new Properties();
    try (FileInputStream input = new FileInputStream(manifest.toFile())) {
      values.load(input);
    }
    require("1".equals(values.getProperty("bundle.version")), "unsupported bundle version");
    Path hprof = safePath(root, required(values, "hprof.file"));
    String hprofHash = required(values, "hprof.sha256");
    requireSha256(hprof, hprofHash, "HPROF");
    int classCount = Integer.parseInt(required(values, "class.count"));
    List<ClassDefinition> classes = new ArrayList<>();
    for (int i = 1; i <= classCount; i++) {
      String prefix = "class." + i + ".";
      Path artifact = safePath(root, required(values, prefix + "file"));
      String artifactHash = required(values, prefix + "sha256");
      requireSha256(artifact, artifactHash, "class artifact " + i);
      classes.add(new ClassDefinition(i,
          Long.parseLong(required(values, prefix + "loader")),
          required(values, prefix + "name"),
          parseId(required(values, prefix + "hprof_id")), artifact, artifactHash));
    }
    return new HprofCheckpointBundle(root, hprof, hprofHash, classes);
  }

  static Path safePath(Path root, String relative) {
    Path supplied = Path.of(relative);
    require(!supplied.isAbsolute(), "bundle artifact path is absolute: " + relative);
    Path resolved = root.resolve(supplied).normalize();
    require(resolved.startsWith(root), "bundle artifact escapes root: " + relative);
    require(Files.isRegularFile(resolved), "bundle artifact is missing: " + relative);
    return resolved;
  }

  static void requireSha256(Path file, String expected, String description) throws IOException {
    String actual = sha256(file);
    require(actual.equalsIgnoreCase(expected),
        description + " SHA-256 mismatch: expected=" + expected + " actual=" + actual);
  }

  static String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      try (java.io.InputStream input = Files.newInputStream(file)) {
        int count;
        while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
      }
      StringBuilder result = new StringBuilder();
      for (byte value : digest.digest()) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError("SHA-256 unavailable", impossible);
    }
  }

  private static String required(Properties values, String key) {
    String value = values.getProperty(key);
    require(value != null && !value.trim().isEmpty(), "missing bundle property " + key);
    return value.trim();
  }

  private static long parseId(String value) {
    return value.startsWith("0x") ? Long.parseUnsignedLong(value.substring(2), 16)
        : Long.parseLong(value);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException("[HPROF-JPF] invalid bundle: " + message);
  }
}
