import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/** Real-HotSpot-only generator for the same-name/different-loader H.1 fixture. */
public final class HprofClassLoaderIdentityDumpGenerator {
  private static final String DUPLICATE_NAME = "hprof.loader.Duplicate";
  private static final String DUPLICATE_RESOURCE = "hprof/loader/Duplicate.class";
  private static volatile Object[] roots;

  private HprofClassLoaderIdentityDumpGenerator() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException(
          "usage: HprofClassLoaderIdentityDumpGenerator <output.hprof>");
    }
    Path output = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(output)) {
      throw new IOException("refusing to overwrite existing heap dump: " + output);
    }

    byte[] bytecode = readDuplicateBytecode();
    DuplicateLoader loaderA = new DuplicateLoader("loader-A", bytecode);
    DuplicateLoader loaderB = new DuplicateLoader("loader-B", bytecode);
    Object instanceA = newInstance(loaderA, 111);
    Object instanceB = newInstance(loaderB, 222);
    if (instanceA.getClass() == instanceB.getClass()
        || instanceA.getClass().getClassLoader() == instanceB.getClass().getClassLoader()) {
      throw new AssertionError("custom loaders did not produce distinct runtime classes");
    }
    roots = new Object[] {loaderA, loaderB, instanceA, instanceB};

    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(output.toString(), true);
    System.out.println("[HPROF-JPF] HotSpot duplicate-class capture: name=" + DUPLICATE_NAME
        + " loaderA=" + loaderA + " markerA=111 loaderB=" + loaderB + " markerB=222 path="
        + output);
  }

  private static Object newInstance(ClassLoader loader, int marker) throws Exception {
    Class<?> type = Class.forName(DUPLICATE_NAME, true, loader);
    Object instance = type.getDeclaredConstructor().newInstance();
    Field field = type.getField("marker");
    field.setInt(instance, marker);
    return instance;
  }

  private static byte[] readDuplicateBytecode() throws IOException {
    try (InputStream input = ClassLoader.getSystemResourceAsStream(DUPLICATE_RESOURCE)) {
      if (input == null) {
        throw new IOException("missing fixture bytecode resource " + DUPLICATE_RESOURCE);
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    }
  }

  private static final class DuplicateLoader extends ClassLoader {
    private final String label;
    private final byte[] bytecode;

    DuplicateLoader(String label, byte[] bytecode) {
      super(null);
      this.label = label;
      this.bytecode = bytecode.clone();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (!DUPLICATE_NAME.equals(name)) {
        throw new ClassNotFoundException(name);
      }
      return defineClass(name, bytecode, 0, bytecode.length);
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
