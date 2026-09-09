/** State whose method frame is reconstructed at an empty-operand-stack checkpoint. */
public final class HprofFrameStateFixture {
  static int preCounter;
  static int result;
  static int continuationCounter;
  static Marker captureMarker;

  static void checkpointMethod() {
    preCounter++;
    int a = 5;
    Marker ref = captureMarker;
    int b = a + 10;
    HprofFrameStateCaptureBarrier.awaitCapture();
    result = b + ref.value;
    continuationCounter++;
  }

  static final class Marker {
    final int value;
    Marker(int value) { this.value = value; }
  }
}
