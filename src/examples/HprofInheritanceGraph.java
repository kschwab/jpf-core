/** Inheritance field-layout definitions shared by HotSpot and modeled JPF. */
public final class HprofInheritanceGraph {
  static InheritanceGraph root;

  private HprofInheritanceGraph() {}

  static final class Ref {
    int id;
  }

  static class Base {
    int baseValue;
    Ref baseRef;
  }

  static final class Derived extends Base {
    int derivedValue;
    Ref derivedRef;
  }

  static class HiddenBase {
    int value;
  }

  static final class HiddenDerived extends HiddenBase {
    int value;
  }

  static final class InheritanceGraph {
    int marker;
    Derived derived;
    HiddenDerived hidden;
  }
}
