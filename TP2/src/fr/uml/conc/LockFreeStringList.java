package fr.uml.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Exercice 2 - Liste chainée avec ajout en fin
 * 
 * @author melissa
 *
 */
public class LockFreeStringList {
  // LockFreeStringList peut voir Entry
  static final class Entry {
    private final String element;
    private volatile Entry next; // relecture en RAM : résout size thread safe

    Entry(String element) {
      this.element = element;
    }
  }

  private final Entry head; // volatile ou getVolatile après
  // private Entry tail;

  /* VAR HANDLE */

  private static final VarHandle NEXT_HANDLE;

  static {
    var lookup = MethodHandles.lookup();
    try {
      // findVarHandle​(Class<?> recv,String name, Class<?> type)
      NEXT_HANDLE = lookup.findVarHandle(Entry.class, "next", Entry.class);
    } catch (NoSuchFieldException | IllegalAccessException e) { // 2 cas qui ne devrait pas se
                                                                // produire
      throw new AssertionError(e); // pas censé être là
    }
  }

  public LockFreeStringList() {
    /* tail = */ head = new Entry(null); // fake first entry
  }

  public void addLast(String element) {
    var newLast = new Entry(element);
    var currentLast = head;
    while (true) {
      var next = currentLast.next; // volatile read
      if (next == null) { // dernier maillon
        // change le champ next de l'objet last
        // change le champ next de l'objet last
        // insertion du maillon newLast
        // si currentLast == null
        if (NEXT_HANDLE.compareAndSet(currentLast, null, newLast)) { 
          return;
        }

        next = currentLast.next; // si ca ne fonctionne pas, last.next n'est plus null : on passe au
                                 // suivant
      }
      currentLast = next; // déplace le dernier maillon
    }
  }

  public int size() {
    // next est volatile
    var count = 0;
    for (var e = head.next; e != null; e = e.next) {
      count++;
    }
    return count;
  }

  private static Runnable createRunnable(LockFreeStringList list, int id) {
    return () ->
    {
      for (var j = 0; j < 10_000; j++) {
        list.addLast(id + " " + j);
      }
    };
  }

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    var threadCount = 5;
    var list = new LockFreeStringList();
    var tasks = IntStream
      .range(0, threadCount)
      .mapToObj(id -> createRunnable(list, id))
      .map(Executors::callable)
      .collect(Collectors.toList());
    var executor = Executors.newFixedThreadPool(threadCount);
    var futures = executor.invokeAll(tasks);
    executor.shutdown();
    for (var future : futures) {
      future.get();
    }
    System.out.println(list.size());
  }
}

/**
 * VarHandle permet de récupérer par référence (& en C). Changer les valeurs d'une case mémoire dans
 * la RAM. Besoin d'un seul VarHandle pour changer plusieurs case si elles sont du même type.
 **/
