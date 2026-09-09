import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Modeled-Java probe for the minimum usable JPF custom-loader sequence. */
public final class HprofJpfClassLoaderProbe {
  private static final String NAME = "hprof.loader.Duplicate";
  private static final String RESOURCE = "hprof/loader/Duplicate.class";

  private HprofJpfClassLoaderProbe() {}

  public static void main(String[] args) throws Exception {
    byte[] bytecode = readBytecode();
    DefiningLoader loaderA = new DefiningLoader();
    DefiningLoader loaderB = new DefiningLoader();
    Class<?> classA = loaderA.define(bytecode);
    Class<?> classB = loaderB.define(bytecode);

    check(classA != classB, "duplicate definitions collapsed to one Class");
    check(classA.getName().equals(classB.getName()), "binary names differ");
    check(classA.getClassLoader() == loaderA, "class A has wrong loader");
    check(classB.getClassLoader() == loaderB, "class B has wrong loader");
    Object instanceA = classA.getDeclaredConstructor().newInstance();
    Object instanceB = classB.getDeclaredConstructor().newInstance();
    check(instanceA.getClass() == classA && instanceB.getClass() == classB,
        "instances lost loader-qualified class identity");
    System.out.println("[HPROF-JPF-MODEL] duplicate-name custom Class definitions verified");
  }

  private static byte[] readBytecode() throws Exception {
    InputStream input = ClassLoader.getSystemResourceAsStream(RESOURCE);
    check(input != null, "missing Duplicate class resource");
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    } finally {
      input.close();
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static final class DefiningLoader extends ClassLoader {
    Class<?> define(byte[] bytes) {
      return defineClass(NAME, bytes, 0, bytes.length);
    }
  }
}
