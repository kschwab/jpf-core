package gov.nasa.jpf.vm.hprof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class HprofCheckpointMetadataTest {
  @Test
  public void acceptsVersionOneLifecycleValues() throws Exception {
    HprofCheckpointMetadata metadata = load(
        "checkpoint.version=1\nclass.example.Foo=UNINITIALIZED\nclass.example.Bar=INITIALIZED\n");
    assertEquals(HprofCheckpointMetadata.ClassLifecycle.UNINITIALIZED,
        metadata.getClassLifecycle("example.Foo"));
    assertEquals(HprofCheckpointMetadata.ClassLifecycle.INITIALIZED,
        metadata.getClassLifecycle("example.Bar"));
  }

  @Test
  public void rejectsMissingVersion() throws Exception {
    reject("class.Foo=INITIALIZED\n", "missing checkpoint.version");
  }

  @Test
  public void rejectsUnsupportedVersion() throws Exception {
    reject("checkpoint.version=999\nclass.Foo=INITIALIZED\n",
        "unsupported checkpoint metadata version 999");
  }

  @Test
  public void rejectsUnknownLifecycle() throws Exception {
    reject("checkpoint.version=1\nclass.Foo=SOMETHING_ELSE\n",
        "unknown lifecycle SOMETHING_ELSE for Foo");
  }

  @Test
  public void rejectsDuplicateLifecycleEntry() throws Exception {
    reject("checkpoint.version=1\nclass.Foo=INITIALIZED\nclass.Foo=UNINITIALIZED\n",
        "duplicate key: class.Foo");
  }

  @Test
  public void rejectsUnknownKey() throws Exception {
    reject("checkpoint.version=1\ngarbage.key=value\n",
        "unknown checkpoint metadata key garbage.key");
  }

  @Test
  public void rejectsEmptyClassIdentity() throws Exception {
    reject("checkpoint.version=1\nclass.=INITIALIZED\n",
        "unknown checkpoint metadata key class.");
  }

  @Test
  public void rejectsEmptyLifecycleValue() throws Exception {
    reject("checkpoint.version=1\nclass.Foo=\n", "empty key or value");
  }

  private static HprofCheckpointMetadata load(String text) throws Exception {
    File file = File.createTempFile("hprof-checkpoint-metadata-", ".properties");
    try {
      Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
      return HprofCheckpointMetadata.load(file);
    } finally {
      assertTrue(file.delete() || !file.exists());
    }
  }

  private static void reject(String text, String expectedMessage) throws Exception {
    try {
      load(text);
      fail("expected invalid metadata to be rejected");
    } catch (IllegalArgumentException expected) {
      assertTrue("diagnostic was: " + expected.getMessage(),
          expected.getMessage().contains(expectedMessage));
      assertTrue("diagnostic should identify the metadata file",
          expected.getMessage().contains("hprof-checkpoint-metadata-"));
    }
  }
}
