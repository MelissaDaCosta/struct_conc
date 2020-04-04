package fr.umlv.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Exercice 2 - SpinLock Réentrant
 * 
 * @author melissa
 *
 */

public class ReentrantSpinLock {
  private volatile int lock;
  // private volatile Thread ownerThread;
  // le champ ownerthread peut ne pas être volatile
  // RAPPEL : si écriture volatile : garantie que toutes les autres écritures sont fait en RAM avant
  private Thread ownerThread;

  private static final VarHandle HANDLE;
  static {
    var lookup = MethodHandles.lookup();
    try {
      HANDLE = lookup.findVarHandle(ReentrantSpinLock.class, "lock", int.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  public void lock() {
    // on récupère la thread courante
    // savoir si on est la thread qui a déjà pris le lock ou pas
    var currentThread = Thread.currentThread();

    while (true) {
      // si lock est == à 0, on utilise un CAS pour le mettre à 1 et
      if (HANDLE.compareAndSet(this, 0, 1)) {
        // on sauvegarde la thread qui possède le lock dans ownerThread.
        this.ownerThread = currentThread;   // ca marche !
        // cette ecriture peut ne pas être vue par d'autre thread
        // mais c'est pas grave car elle est lue par le thread courant obligatoirement
        return;
      }
      // sinon on regarde si la thread courante n'est pas ownerThread,
      // lecture volatile
      if (this.ownerThread == currentThread) {
        // si oui alors on incrémente lock.
        // une seule thread peut rentrer dedans donc on peut faire ++
        this.lock++;    // écriture volatile : toutes les autres écritures ont été faites en RAM avant
        return;
      }
      // et il faut une boucle pour retenter le CAS après avoir appelé onSpinWait()
      Thread.onSpinWait();
    }
  }


  public void unlock() {
    // idée de l'algo
    // si la thread courante est != ownerThread
    if (this.ownerThread != Thread.currentThread()) {
      // on pète une exception
      throw new IllegalStateException();
    }
    // ici on est le owner thread, pas d'autre thread qui peuvent passer
    var lockVolatile = this.lock; // lecture volatile
    // pour éviter de faire plein de traffic en lecture et écriture volatil qui coutent chere
    // on stock dans un variable intermédiaire pour éviter ca
    // si lock == 1
    if (lockVolatile == 1) {
      // on remet ownerThread à null
      this.ownerThread = null; // rend le lock
      // ecriture volatile: donc pas besoin de ownerthread volatile
      // car garantie que les écritures d'avant ont été faites en RAM avant
      this.lock = 0;
      return;
    }
    // on décrémente lock
    this.lock = lockVolatile - 1; // ecriture volatile
  }

  public static void main(String[] args) throws InterruptedException {
    var runnable = new Runnable() {
      private int counter;
      private final ReentrantSpinLock spinLock = new ReentrantSpinLock();

      @Override
      public void run() {
        for (var i = 0; i < 1_000_000; i++) {
          spinLock.lock();
          try {
            spinLock.lock();
            try {
              counter++;
            } finally {
              spinLock.unlock();
            }
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
    System.out.println("counter " + runnable.counter); // ON a bien 2M
  }
}
