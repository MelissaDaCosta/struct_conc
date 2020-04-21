package fr.umlv.conc;

import java.util.Map.Entry;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Objects;

/* Exercice 3 - Liste chaînée lock-free */

public class Linked2<E> {
  private static class Entry<E> {
    private final E element; // nom mutable pas de soucis thread safe
    private final Entry<E> next;

    private Entry(E element, Entry<E> next) {
      this.element = element;
      this.next = next;
    }
  }

  private Entry<E> head;

  private static final VarHandle VARHANDLE;

  static {
    var lookup = MethodHandles.lookup();
    try {
      VARHANDLE = lookup.findVarHandle(Linked2.class, "head", Entry.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  public void addFirst(E element) {
    Objects.requireNonNull(element);

    for (;;) {
      var currentHead = head; // si head n'est pas volatile VARHANDLE.getVolatile(this)
      var newHead = new Entry<>(element, currentHead);
      if (VARHANDLE.compareAndSet(this, currentHead, newHead)) {
        return;
      }
    }
  }

  public int size() {
    var size = 0;
    // head est un champ, rien ne garantit qu'on a la bonne valeur : pas thread safe
    for (var link = head; link != null; link = link.next) {
      size++;
    }
    return size;
  }

  public static void main(String[] args) throws InterruptedException {
    var linked = new Linked2<String>();
    var threads = new ArrayList<Thread>();

    for (var i = 0; i < 4; i++) {
      var thread = new Thread(() ->
      {
        for (var j = 0; j < 100_000; j++) {
          linked.addFirst("toto");
        }
      });
      thread.start();
      threads.add(thread);
    }

    for (var thread : threads) {
      thread.join();
    }

    System.out.println(linked.size());
  }
}

/**
 * 1. Le code n'est pas thread safe car l'opération qui modifie n'est pas protégé
 * 
 */
