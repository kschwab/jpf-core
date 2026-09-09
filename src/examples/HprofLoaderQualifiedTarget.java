import java.lang.reflect.Method;

/** Modeled validation that executes both imported same-name class definitions. */
public final class HprofLoaderQualifiedTarget {
  private HprofLoaderQualifiedTarget() {}

  public static void main(String[] args) throws Exception {
    Object first = HprofLoaderQualifiedRoots.first;
    Object second = HprofLoaderQualifiedRoots.second;
    check(first != null && second != null && first != second, "imported roots are invalid");
    check(first.getClass().getName().equals(second.getClass().getName()), "binary names differ");
    check(first.getClass() != second.getClass(), "loader-qualified Classes were collapsed");
    check(first.getClass().getClassLoader() != second.getClass().getClassLoader(),
        "loader-qualified Classes use one loader");

    check(invoke(first, "getMarker") == 1111, "captured marker A was not restored");
    check(invoke(second, "getMarker") == 2222, "captured marker B was not restored");
    check(invoke(first, "definitionMarker") == 111, "definition A executed wrong bytecode");
    check(invoke(second, "definitionMarker") == 222, "definition B executed wrong bytecode");
    System.out.println(
        "[HPROF-JPF-MODEL] loader-qualified imported instances verified: "
            + "fields={1111,2222} methods={111,222}");
  }

  private static int invoke(Object target, String name) throws Exception {
    Method method = target.getClass().getMethod(name);
    return (Integer) method.invoke(target);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
