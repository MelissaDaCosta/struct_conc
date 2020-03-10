package fr.umlv.conc;

import java.util.ArrayList;

/**
 * Exercice 2 - Compteur
 * 1
 * @author mdacos05
 *
 */

public class Counter {
	// private int counter;
	// cela ne change rien, forcer les aller-retour en RAM ne change rien
	private volatile int counter;	

	public int nextInt() {
		return counter++;
	}

	public static void main(String[] args) throws InterruptedException {
		var counter = new Counter();
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
 * 2.
 * En déclarant le champ volatile cela ne change rien, le pb ne vient pas de là
 * 
 * 3.
 *  CAS(&field, expectedValue, newValue)->boolean  
 * Le boolean de retour de la méthode CompareAndSet retourne true si
 * le contenue de field ===  expectedValue
 * 
 * 
 */
