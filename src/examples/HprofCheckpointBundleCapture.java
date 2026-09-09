import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Real-HotSpot capture producing the unfinalized H.3 checkpoint bundle. */
public final class HprofCheckpointBundleCapture {
  private static final String BINARY_NAME = "hprof.loader.Duplicate";
  private static volatile Object[] roots;

  private HprofCheckpointBundleCapture() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: HprofCheckpointBundleCapture <bundle-dir>");
    }
    Path bundle = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(bundle)) {
      throw new IOException("refusing to overwrite checkpoint bundle: " + bundle);
    }
    Path classes = bundle.resolve("classes");
    Path work = bundle.resolve(".capture-work");
    Files.createDirectories(classes);
    byte[] bytesA = compile(work.resolve("a"), 111);
    byte[] bytesB = compile(work.resolve("b"), 222);
    Path artifactA = classes.resolve("class-1.class");
    Path artifactB = classes.resolve("class-2.class");
    Files.write(artifactA, bytesA);
    Files.write(artifactB, bytesB);

    DefinitionLoader loaderA = new DefinitionLoader("bundle-loader-1");
    DefinitionLoader loaderB = new DefinitionLoader("bundle-loader-2");
    Class<?> classA = loaderA.define(bytesA);
    Class<?> classB = loaderB.define(bytesB);
    Object instanceA = classA.getDeclaredConstructor().newInstance();
    Object instanceB = classB.getDeclaredConstructor().newInstance();
    require(invokeMarker(instanceA) == 111 && invokeMarker(instanceB) == 222,
        "different captured definitions did not execute distinct behavior");

    LoaderTag loaderTagA = new LoaderTag(1, loaderA);
    LoaderTag loaderTagB = new LoaderTag(2, loaderB);
    ClassTag classTagA = new ClassTag(1, 1, loaderA);
    ClassTag classTagB = new ClassTag(2, 2, loaderB);
    roots = new Object[] {
        loaderTagA, loaderTagB, classTagA, classTagB, instanceA, instanceB
    };

    Path heap = bundle.resolve("heap.hprof");
    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
        .dumpHeap(heap.toString(), true);
    System.out.println("[HPROF-JPF] checkpoint bundle capture: loaders={1,2} classes={1,2} "
        + "behaviors={111,222} path=" + bundle);
  }

  private static byte[] compile(Path work, int marker) throws IOException {
    Path source = work.resolve("src/hprof/loader/Duplicate.java");
    Path output = work.resolve("out");
    Files.createDirectories(source.getParent());
    Files.createDirectories(output);
    String text = "package hprof.loader; public class Duplicate { "
        + "public int definitionMarker() { return " + marker + "; } }\n";
    Files.write(source, text.getBytes(StandardCharsets.UTF_8));
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException("checkpoint capture requires a JDK JavaCompiler");
    }
    int result = compiler.run(null, null, null, "-g:none", "-d", output.toString(),
        source.toString());
    if (result != 0) {
      throw new IllegalStateException("fixture compilation failed for marker " + marker);
    }
    return Files.readAllBytes(output.resolve("hprof/loader/Duplicate.class"));
  }

  private static int invokeMarker(Object instance) throws Exception {
    Method method = instance.getClass().getMethod("definitionMarker");
    return (Integer) method.invoke(instance);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class DefinitionLoader extends ClassLoader {
    private final String label;

    DefinitionLoader(String label) {
      super(null);
      this.label = label;
    }

    Class<?> define(byte[] bytes) {
      return defineClass(BINARY_NAME, bytes, 0, bytes.length);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  static final class LoaderTag {
    final long logicalLoaderId;
    final ClassLoader loader;

    LoaderTag(long logicalLoaderId, ClassLoader loader) {
      this.logicalLoaderId = logicalLoaderId;
      this.loader = loader;
    }
  }

  static final class ClassTag {
    final long logicalClassId;
    final long logicalLoaderId;
    final ClassLoader loader;

    ClassTag(long logicalClassId, long logicalLoaderId, ClassLoader loader) {
      this.logicalClassId = logicalClassId;
      this.logicalLoaderId = logicalLoaderId;
      this.loader = loader;
    }
  }
}
