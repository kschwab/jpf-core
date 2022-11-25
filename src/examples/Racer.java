import java.util.concurrent.locks.ReentrantLock;
public class Racer implements Runnable {
     int sharedData = 10;
     private ReentrantLock mutex = new ReentrantLock();

	public void run () {
          try { Thread.sleep(1); } catch (InterruptedException ix) {}
          mutex.lock();
          sharedData = 0;
          mutex.unlock();
     }

     public static void main (String[] args){
          Racer racer = new Racer();
          new Thread(racer).start();
          try { Thread.sleep(1); } catch (InterruptedException ix) {}
          racer.mutex.lock();
          int divByZeroRaceCondition = 100 / racer.sharedData;
          racer.mutex.unlock();
     }
}