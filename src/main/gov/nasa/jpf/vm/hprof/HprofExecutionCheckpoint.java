package gov.nasa.jpf.vm.hprof;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Experimental execution.version=1 model for one RUNNING Java frame. */
public final class HprofExecutionCheckpoint {
  public enum SlotKind {
    INT,
    REFERENCE_HPROF
  }

  public static final class LocalSlot {
    public final int index;
    public final SlotKind kind;
    public final long value;

    LocalSlot(int index, SlotKind kind, long value) {
      this.index = index;
      this.kind = kind;
      this.value = value;
    }
  }

  public final long logicalThreadId;
  public final String threadState;
  public final String className;
  public final String methodName;
  public final String descriptor;
  public final int bytecodeOffset;
  public final int localCount;
  public final List<LocalSlot> locals;

  private HprofExecutionCheckpoint(
      long thread,
      String state,
      String cls,
      String method,
      String descriptor,
      int pc,
      int count,
      List<LocalSlot> locals) {
    logicalThreadId = thread;
    threadState = state;
    className = cls;
    methodName = method;
    this.descriptor = descriptor;
    bytecodeOffset = pc;
    localCount = count;
    this.locals = Collections.unmodifiableList(locals);
  }

  public static HprofExecutionCheckpoint load(File file) throws IOException {
    Properties properties = new Properties();
    try (FileInputStream in = new FileInputStream(file)) {
      properties.load(in);
    }
    require("1".equals(required(properties, "execution.version")), "unsupported execution version");
    long threadId = Long.parseLong(required(properties, "thread.1.logical_id"));
    String state = required(properties, "thread.1.state");
    require("RUNNING".equals(state), "only RUNNING is supported");

    int count = Integer.parseInt(required(properties, "frame.1.local_count"));
    List<LocalSlot> locals = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String raw = required(properties, "frame.1.local." + i);
      int split = raw.indexOf(':');
      require(split > 0, "malformed local " + i);
      SlotKind kind;
      try {
        kind = SlotKind.valueOf(raw.substring(0, split));
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "[HPROF-JPF] invalid execution metadata: unknown local kind " + raw, ex);
      }
      String value = raw.substring(split + 1);
      long parsed =
          value.startsWith("0x")
              ? Long.parseUnsignedLong(value.substring(2), 16)
              : Long.parseLong(value);
      locals.add(new LocalSlot(i, kind, parsed));
    }

    return new HprofExecutionCheckpoint(
        threadId,
        state,
        required(properties, "frame.1.class"),
        required(properties, "frame.1.method"),
        required(properties, "frame.1.descriptor"),
        Integer.parseInt(required(properties, "frame.1.bytecode_offset")),
        count,
        locals);
  }

  private static String required(Properties properties, String key) {
    String value = properties.getProperty(key);
    require(value != null && !value.trim().isEmpty(), "missing " + key);
    return value.trim();
  }

  private static void require(boolean value, String message) {
    if (!value) {
      throw new IllegalArgumentException("[HPROF-JPF] invalid execution metadata: " + message);
    }
  }
}
