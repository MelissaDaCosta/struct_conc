package fr.umlv.conc;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercice 2 - Compteur
 * 2
 * @author mdacos05
 *
 */

public class CounterAtomicInteger {
	// private int counter;
	// private volatile int counter;
	// cela ne change rien, forcer les aller-retour en RAM ne change rien
	private final AtomicInteger counter = new AtomicInteger();

	public int nextInt() {
		while (true) {
			var currentValue = counter.get();
			if (counter.compareAndSet(currentValue, currentValue + 1)) {
				return currentValue;
			}
			// else : on repasse dans la boucle
		}

	}

	public static void main(String[] args) throws InterruptedException {
		var counter = new CounterAtomicInteger();
		var threads = new ArrayList<Thread>();

		for (var i = 0; i < 4; i++) {
			var thread = new Thread(() -> {
				for (var j = 0; j < 100_000; j++) {
					counter.nextInt();
				}
			});
			thread.start();
			threads.add(thread);
		}

		for (var thread : threads) {
			thread.join();
		}

		System.out.println(counter.counter);
	}
}

/**
 * Le code n'est pas thread-safe car dans la méthode nextInt, on ne relit pas la
 * dernière valeur
 * 
 * Avec 4 thread qui tourne 100 000 fois on devrait avoir d'afficher 400 000.
 * Donc clairement c'est pas thread safe
 * 
 * 
 * 3. CAS(&field, expectedValue, newValue)->boolean Le boolean de retour de la
 * méthode CompareAndSet retourne true si le contenue de field === expectedValue
 * 
 * 
 */
