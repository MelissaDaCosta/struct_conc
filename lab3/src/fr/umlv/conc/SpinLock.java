package fr.umlv.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Exercice 1 - SpinLock pas réentrant
 * @author melissa
 *
 */

public class SpinLock {
  private volatile boolean isLocked;
  
  private static final VarHandle HANDLE;
  static {
    var lookup = MethodHandles.lookup();
    try {
      HANDLE = lookup.findVarHandle(SpinLock.class, "isLocked", boolean.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }


  /**
   * onSpinWait​() Indicates that the caller is momentarily unable to progress, until the occurrence
   * of one or more actions on the part of other activities. By invoking this method within each
   * iteration of a spin-wait loop construct, the calling thread indicates to the runtime that it is
   * busy-waiting. The runtime may take action to improve the performance of invoking spin-wait loop
   * constructions.
   */

  public void lock() {
    // attendre tant que le lock est déjà pris -> attente active
    while (!HANDLE.compareAndSet(this, false, true)) { // passer de faux à vrai
      Thread.onSpinWait();// résout l'attente active -> ne veut pas que ce thread soit schédulé par
                          // l'os
    }
    // si CaS renvoie vrai -> on a le lock, sinon on boucle : on se met en pause
  }
  
  public boolean tryLock() {
    return HANDLE.compareAndSet(this, false, true);
    // renvoie true si ca a marché et du coup ca prend aussi le lock
    // si cas renvoie false, tryLock renvoie aussi false et on a pas le lock
  }

  public void unlock() {
    this.isLocked = false; // écriture volatile
  }

  public static void main(String[] args) throws InterruptedException {
    var runnable = new Runnable() {
      private int counter;
      private final SpinLock spinLock = new SpinLock();

      @Override
      public void run() {
        for (int i = 0; i < 1_000_000; i++) {
          spinLock.lock();
          try {
            counter++;
          } finally {
            spinLock.unlock();
          }
        }
      }
    };
    var t1 = new Thread(runnable);
    var t2 = new Thread(runnable);
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    System.out.println("counter : " + runnable.counter); // On a bien 2M
  }
}
