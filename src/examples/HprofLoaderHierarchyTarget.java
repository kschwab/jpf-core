import java.lang.reflect.Method;

/** Modeled execution over the imported custom-loader superclass/interface graph. */
public final class HprofLoaderHierarchyTarget {
  public static void main(String[] args) throws Exception {
    Object root = HprofLoaderHierarchyRoots.root;
    check(root != null, "missing imported Derived root");
    Class<?> derived = root.getClass();
    check("hprof.loader.Derived".equals(derived.getName()), "wrong runtime class");
    check(invoke(root, "getBaseField") == 1111, "inherited field mismatch");
    check(invoke(root, "getDerivedField") == 2222, "derived field mismatch");
    check(invoke(root, "callBase") == 101, "virtual base dispatch mismatch");
    check(invoke(root, "derivedMethod") == 202, "derived virtual dispatch mismatch");
    check(invoke(root, "callInterface") == 303, "interface dispatch mismatch");
    System.out.println("[HPROF-JPF-MODEL] loader-qualified class hierarchy verified: fields={1111,2222} virtual={101,202} interface=303");
  }
  private static int invoke(Object target, String name) throws Exception { Method method=target.getClass().getMethod(name); return (Integer)method.invoke(target); }
  private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
