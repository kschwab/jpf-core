/** HotSpot-only coordination point; restored JPF execution starts after its invocation. */
public final class HprofFrameStateCaptureBarrier {
  static volatile boolean ready;
  static volatile boolean captured;
  static void awaitCapture() {
    ready = true;
    while (!captured) Thread.onSpinWait();
  }
}
