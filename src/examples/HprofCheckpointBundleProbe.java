import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

/** Modeled JPF probe executing the two packaged H.3 class definitions. */
public final class HprofCheckpointBundleProbe {
  private static final String NAME = "hprof.loader.Duplicate";

  private HprofCheckpointBundleProbe() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new AssertionError("expected two class artifact paths");
    DefinitionLoader loaderA = new DefinitionLoader();
    DefinitionLoader loaderB = new DefinitionLoader();
    Class<?> classA = loaderA.define(read(args[0]));
    Class<?> classB = loaderB.define(read(args[1]));
    Object instanceA = classA.getDeclaredConstructor().newInstance();
    Object instanceB = classB.getDeclaredConstructor().newInstance();
    check(invoke(instanceA) == 111, "class artifact 1 returned wrong marker");
    check(invoke(instanceB) == 222, "class artifact 2 returned wrong marker");
    check(classA != classB && classA.getClassLoader() != classB.getClassLoader(),
        "packaged definitions lost loader identity");
    System.out.println("[HPROF-JPF-MODEL] packaged class definitions executed: values={111,222}");
  }

  private static byte[] read(String file) throws Exception {
    try (InputStream input = new FileInputStream(file)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int count;
      while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
      return output.toByteArray();
    }
  }

  private static int invoke(Object instance) throws Exception {
    Method method = instance.getClass().getMethod("definitionMarker");
    return (Integer) method.invoke(instance);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class DefinitionLoader extends ClassLoader {
    Class<?> define(byte[] bytes) {
      return defineClass(NAME, bytes, 0, bytes.length);
    }
  }
}
