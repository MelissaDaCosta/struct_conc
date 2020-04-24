package fr.umlv.conc;

import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Objects;

/* Exercice 3 - Liste chaînée lock-free */

public class Linked<E> {
	private static class Entry<E> {
		private final E element; // nom mutable pas de soucis thread safe
		private final Entry<E> next;

		private Entry(E element, Entry<E> next) {
			this.element = element;
			this.next = next;
		}
	}

	private Entry<E> head;

	public void addFirst(E element) {
		// si plusieurs  threads qui appellent addFirst : on va sauté des maillon
		Objects.requireNonNull(element);
		// d'abord on lit head puis on écrit head
		head = new Entry<>(element, head);
		
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
		var linked = new Linked<String>();
		var threads = new ArrayList<Thread>();
		
		// lance 4 threads
		for (var i = 0; i < 4; i++) {
			var thread = new Thread(() -> {
				for (var j = 0; j < 100_000; j++) {
					linked.addFirst("toto");
				}
			});
			thread.start();
			threads.add(thread);
		}

		// attands que tous les threads se terminent
		for (var thread : threads) {
			thread.join();
		}

		// n'affiche jamais une taille de 400 000 ce qui devrait être le cas
		System.out.println(linked.size());
	}
}

/**
 * 1.
 * Le code n'est pas thread safe car l'opération qui modifie n'est pas protégé
 * 
 */