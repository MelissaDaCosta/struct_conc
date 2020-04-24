package fr.umlv.conc;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.Objects;

/* Exercice 3 - Liste chaînée lock-free */

public class LinkedAtomicReference<E> {
	private static class Entry<E> {
		private final E element; // nom mutable pas de soucis thread safe
		private final Entry<E> next;

		private Entry(E element, Entry<E> next) {
			this.element = element;
			this.next = next;
		}
	}

	// initialise par défaut à null
	private final AtomicReference<Entry<E>> head = new AtomicReference<>();

	public void addFirst(E element) {
		Objects.requireNonNull(element);
		// tant qu'on a pas ajouté le nouveau maillon
		while (true) {
			var currentHead = head.get();
			// pas le choix de faire de l'allocation
			var newHead = new Entry<E>(element, currentHead);

			if (head.compareAndSet(currentHead, newHead)) {
				return;
			}
		}
	}

	public int size() {
		var size = 0;
		// head est un champ, rien ne garantit qu'on a la bonne valeur : pas thread safe
		// head.get pour lire la valeur de atomic reference
		for (var link = head.get(); link != null; link = link.next) {
			size++;
		}
		return size;
	}

	public static void main(String[] args) throws InterruptedException {
		var linked = new LinkedAtomicReference<String>();
		var threads = new ArrayList<Thread>();

		for (var i = 0; i < 4; i++) {
			var thread = new Thread(() -> {
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
 * 1. Le code n'est pas thread safe car l'opération qui modifie n'est pas
 * protégé
 * 
 * 3. La classe AtomicReference n'est pas super efficace
 * car il y a une indirection en plus pour accéder à E.
 * 
 * 
 */