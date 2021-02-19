
// enve javac HelloWorld.java
// enve jpf HelloWorld.jpf
// enve jpf +classpath=. HelloWorld
// enve jpf +classpath=. +listener=MyListener HelloWorld

// Create a deadlock

import java.util.Random;

class Talk implements Runnable
{
    private int threadNum;
    private Random rand;

    Talk(int threadNum)
    {
        this.threadNum = threadNum;
        rand = new Random();
    }

    @Override
    public void run()
    {
        for (int talk = 0; talk < 3; talk++)
        {
            System.out.println("Hi, my name is Thread " + threadNum);

            double prevSharedData = HelloWorld.sharedData;
            HelloWorld.sharedData += rand.nextInt(16) * threadNum;
            System.out.println("  [" + threadNum + "] Updated Shared Data: " +
                               prevSharedData + " -> " + HelloWorld.sharedData);
        }
    }
}

class HelloWorld
{
    public static double sharedData;

    public static void main(String[] args)
    {
        System.out.println("Hello, World!");

        Thread[] threads = new Thread[4];

        for (int threadNum = 0; threadNum < threads.length; threadNum++)
        {
            threads[threadNum] = new Thread(new Talk(threadNum));
            threads[threadNum].start();
        }

        System.out.println("Goodbye, World!");
    }
}
