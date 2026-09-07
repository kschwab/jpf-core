/** Shared class-initialization characterization fixture. */
public final class HprofClassInitState {
  private HprofClassInitState() {}

  static final class InitEffects {
    static int effectCount;
  }

  static final class InitSubject {
    static int value;

    static {
      value = 0;
      InitEffects.effectCount++;
    }
  }
}
