import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Real-HotSpot capture for the H.5 custom-loader hierarchy checkpoint. */
public final class HprofLoaderHierarchyCapture {
  private static volatile Object[] roots;

  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("usage: HprofLoaderHierarchyCapture <bundle-dir>");
    Path bundle = Path.of(args[0]).toAbsolutePath().normalize();
    if (Files.exists(bundle)) throw new IOException("refusing to overwrite checkpoint bundle: " + bundle);
    Path artifacts = bundle.resolve("classes");
    Path work = bundle.resolve(".capture-work");
    Files.createDirectories(artifacts);
    Map<String, byte[]> definitions = compile(work);
    String[] names = {"hprof.loader.Contract", "hprof.loader.Base", "hprof.loader.Derived"};
    for (int i = 0; i < names.length; i++) Files.write(artifacts.resolve("class-" + (i + 1) + ".class"), definitions.get(names[i]));

    DefinitionLoader loader = new DefinitionLoader(definitions);
    Class<?> derivedClass = loader.loadClass("hprof.loader.Derived");
    Object instance = derivedClass.getDeclaredConstructor().newInstance();
    derivedClass.getField("baseField").setInt(instance, 1111);
    derivedClass.getField("derivedField").setInt(instance, 2222);
    require((Integer) derivedClass.getMethod("baseMethod").invoke(instance) == 101, "base method mismatch");
    require((Integer) derivedClass.getMethod("derivedMethod").invoke(instance) == 202, "derived method mismatch");
    require((Integer) derivedClass.getMethod("callInterface").invoke(instance) == 303, "interface dispatch mismatch");
    loader.releaseDefinitionBytes();

    roots = new Object[] {new LoaderTag(1, loader),
        new ClassTag(1, 1, loader), new ClassTag(2, 1, loader), new ClassTag(3, 1, loader), instance};
    Path heap = bundle.resolve("heap.hprof");
    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(heap.toString(), true);
    System.out.println("[HPROF-JPF] loader hierarchy capture: loader=1 classes={Contract,Base,Derived} fields={1111,2222}");
  }

  private static Map<String, byte[]> compile(Path work) throws IOException {
    Path sourceRoot = work.resolve("src/hprof/loader");
    Path output = work.resolve("out");
    Files.createDirectories(sourceRoot); Files.createDirectories(output);
    Path contract = sourceRoot.resolve("Contract.java");
    Path base = sourceRoot.resolve("Base.java");
    Path derived = sourceRoot.resolve("Derived.java");
    Files.writeString(contract, "package hprof.loader; public interface Contract { int interfaceMarker(); }\n", StandardCharsets.UTF_8);
    Files.writeString(base, "package hprof.loader; public class Base { public int baseField; public int baseMethod(){return 101;} public int getBaseField(){return baseField;} }\n", StandardCharsets.UTF_8);
    Files.writeString(derived, "package hprof.loader; public class Derived extends Base implements Contract { public int derivedField; public int derivedMethod(){return 202;} public int interfaceMarker(){return 303;} public int getDerivedField(){return derivedField;} public int callBase(){return baseMethod();} public int callInterface(){Contract c=this; return c.interfaceMarker();} }\n", StandardCharsets.UTF_8);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) throw new IllegalStateException("capture requires a JDK JavaCompiler");
    int result = compiler.run(null, null, null, "-g:none", "-d", output.toString(), contract.toString(), base.toString(), derived.toString());
    if (result != 0) throw new IllegalStateException("hierarchy fixture compilation failed");
    Map<String, byte[]> bytes = new LinkedHashMap<>();
    for (String name : new String[]{"Contract", "Base", "Derived"}) bytes.put("hprof.loader." + name, Files.readAllBytes(output.resolve("hprof/loader/" + name + ".class")));
    return bytes;
  }

  private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

  private static final class DefinitionLoader extends ClassLoader {
    private Map<String, byte[]> definitions;
    DefinitionLoader(Map<String, byte[]> definitions) { super(null); this.definitions = new LinkedHashMap<>(definitions); }
    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] bytes = definitions.get(name); if (bytes == null) throw new ClassNotFoundException(name);
      return defineClass(name, bytes, 0, bytes.length);
    }
    void releaseDefinitionBytes() { definitions = null; }
  }
  static final class LoaderTag { final long logicalLoaderId; final ClassLoader loader; LoaderTag(long id, ClassLoader loader){this.logicalLoaderId=id;this.loader=loader;} }
  static final class ClassTag { final long logicalClassId; final long logicalLoaderId; final ClassLoader loader; ClassTag(long id,long loaderId,ClassLoader loader){this.logicalClassId=id;this.logicalLoaderId=loaderId;this.loader=loader;} }
}
