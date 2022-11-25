/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

import gov.nasa.jpf.vm.Verify;

public class RacerInterleave implements Runnable {

     static int raceVariable = 1;
     static int dummySharedVariable = 1;

     boolean isEnableFirstInterleave;
     boolean isEnableRaceVariable;
     boolean isEnableCheckRaceCondition;
     boolean isEnableSecondInterleave;
     int nonRaceVariable = 2;
     public static void main (String[] args) {
          switch (19)
          {
               case -1: testNoInterleave(); break;
               case 0:  testInterleaveMainOnly(); break;
               case 1:  testInterleaveMainAndThread1Only_A(); break;
               case 2:  testInterleaveMainAndThread1Only_B(); break;
               case 3:  testInterleaveMainAndThread2Only_A(); break;
               case 4:  testInterleaveMainAndThread2Only_B(); break;
               case 5:  testInterleaveThread1Only(); break;
               case 6:  testInterleaveThread2Only(); break;
               case 7:  testInterleaveThread1And2Only(); break;
               case 8:  testInterleaveAll_A(); break;
               case 9:  testInterleaveAll_B(); break;

               case 11: testTwoInterleavesMainAndThread1Only_A(); break;
               case 12: testTwoInterleavesMainAndThread1Only_B(); break;
               case 13: testTwoInterleavesMainAndThread2Only_A(); break;
               case 14: testTwoInterleavesMainAndThread2Only_B(); break;
               case 15: testTwoInterleavesThread1Only(); break;
               case 16: testTwoInterleavesThread2Only(); break;
               case 17: testTwoInterleavesThread1And2Only(); break;
               case 18: testTwoInterleavesAll_A(); break;
               case 19: testTwoInterleavesAll_B(); break;
          }
     }

     public RacerInterleave(boolean isEnableFirstInterleave, boolean isEnableRaceVariable, boolean isEnableCheckRaceCondition, boolean isEnableSecondInterleave) {
          this.isEnableFirstInterleave = isEnableFirstInterleave;
          this.isEnableRaceVariable = isEnableRaceVariable;
          this.isEnableCheckRaceCondition = isEnableCheckRaceCondition;
          this.isEnableSecondInterleave = isEnableSecondInterleave;
     }

     public void run () {
          if (isEnableFirstInterleave) {
               Verify.startInterleaving();
          }

          Thread.yield();

          if (isEnableCheckRaceCondition) {
               int checkRaceCondition = 1 / raceVariable;
          }

          if (isEnableRaceVariable) {
               raceVariable = 0;
          }

          if (isEnableFirstInterleave) {
               Verify.stopInterleaving();
          }

          Thread.yield(); // always skipped

          if (isEnableSecondInterleave) {
               Verify.startInterleaving();
          }

          // int x = dummySharedVariable + 1; 
          // dummySharedVariable = x;
          dummySharedVariable++;

          Thread.yield();

          if (isEnableSecondInterleave) {
               Verify.stopInterleaving();
          }
     }

     public static void testNoInterleave() {
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }
     
     public static void testInterleaveMainOnly() {
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testInterleaveMainAndThread1Only_A() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testInterleaveMainAndThread2Only_A() {
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, false));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testInterleaveMainAndThread1Only_B() {
          Verify.startInterleaving();
          Thread t1 = new Thread(new RacerInterleave(true, false, false, false));
          t1.start();
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testInterleaveMainAndThread2Only_B() {
          Verify.startInterleaving();
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          t1.start();
          Thread t2 = new Thread(new RacerInterleave(true, false, false, false));
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testInterleaveThread1Only() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }

     public static void testInterleaveThread2Only() {
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, false));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }

     public static void testInterleaveThread1And2Only() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, false));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }

     public static void testInterleaveAll_A() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, false));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testInterleaveAll_B() {
          Verify.startInterleaving();
          Thread t1 = new Thread(new RacerInterleave(true, false, false, false));
          t1.start();
          Thread t2 = new Thread(new RacerInterleave(true, false, false, false));
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testTwoInterleavesMainAndThread1Only_A() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, true));
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testTwoInterleavesMainAndThread2Only_A() {
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, true));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testTwoInterleavesMainAndThread1Only_B() {
          Verify.startInterleaving();
          Thread t1 = new Thread(new RacerInterleave(true, false, false, true));
          t1.start();
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testTwoInterleavesMainAndThread2Only_B() {
          Verify.startInterleaving();
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          t1.start();
          Thread t2 = new Thread(new RacerInterleave(true, false, false, true));
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testTwoInterleavesThread1Only() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, true));
          Thread t2 = new Thread(new RacerInterleave(false, false, false, false));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }

     public static void testTwoInterleavesThread2Only() {
          Thread t1 = new Thread(new RacerInterleave(false, false, false, false));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, true));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }

     public static void testTwoInterleavesThread1And2Only() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, true));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, true));
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
     }

     public static void testTwoInterleavesAll_A() {
          Thread t1 = new Thread(new RacerInterleave(true, false, false, true));
          Thread t2 = new Thread(new RacerInterleave(true, false, false, true));

          Verify.startInterleaving();
          t1.start();
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }

     public static void testTwoInterleavesAll_B() {
          Verify.startInterleaving();
          Thread t1 = new Thread(new RacerInterleave(true, false, false, true));
          t1.start();
          Thread t2 = new Thread(new RacerInterleave(true, false, false, true));
          t2.start();

          Thread.yield();

          int checkRaceCondition = 1 / raceVariable;
          Verify.stopInterleaving();
     }
}
