/** Starts a normal modeled thread whose initial stack receives the reconstructed captured frame. */
public final class HprofFrameStateTarget {
  public static void main(String[] args) throws Exception {
    Thread worker=new Thread(new EmptyRun(),"hprof-restored-frame");
    worker.start(); worker.join();
    check(HprofFrameStateFixture.preCounter==1,"pre-checkpoint code reexecuted");
    check(HprofFrameStateFixture.result==22,"restored locals/reference produced wrong result");
    check(HprofFrameStateFixture.continuationCounter==1,"continuation did not execute exactly once");
    System.out.println("[HPROF-JPF-MODEL] reconstructed frame continuation verified: preCounter=1 result=22 continuationCounter=1");
  }
  static final class EmptyRun implements Runnable { public void run() {} }
  private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
