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

public class LockFreeStringList2 {
	// LockFreeStringList peut voir Entry
	static final class Entry {
		private final String element;
		private volatile Entry next;		// relecture en RAM : résout size thread safe

		Entry(String element) {
			this.element = element;
		}
	}

	private final Entry head; // volatile ou getVolatile après
	private volatile Entry tail;

	/* VAR HANDLE */

	private static final VarHandle NEXT_HANDLE;
	private static final VarHandle TAIL_HANDLE;

	static {
		var lookup = MethodHandles.lookup();
		try {
			NEXT_HANDLE = lookup.findVarHandle(Entry.class, "next", Entry.class);
			TAIL_HANDLE = lookup.findVarHandle(LockFreeStringList2.class, "tail", Entry.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {	// 2 cas qui ne devrait pas se produire
			throw new AssertionError(e);	// pas censé être là
		}
	}

	public LockFreeStringList2() {
		tail = head = new Entry(null); // fake first entry
	}

	public void addLast(String element) {
		var newLast = new Entry(element);
		var currentLast = tail;	// volatile read
		var oldTail = tail;
		while (true) {
			var next = currentLast.next; // volatile read
			if (next == null) {	// dernier maillon
				// change le champ next de l'objet last
				if (NEXT_HANDLE.compareAndSet(currentLast, null, newLast)) {	// insertion du maillon
					//tail = newLast;
					// si on entoure d'un if pas besoin de cast
					var b = (boolean)TAIL_HANDLE.compareAndSet(this, oldTail, newLast);
					// si ca rate un autre thread a été mis à jour donc on ne doit rien faire
					return ;
				}
				//si ca ne fonctionne pas, plein de maillons on été insérés entreF
				next = tail;	// on recharge tail 	
			}
			currentLast = next;	// déplace le dernier maillon
		}
	}

	public int size() {
		// next est volatile : thread safe
		var count = 0;
		for (var e = head.next; e != null; e = e.next) {
			count++;
		}
		return count;
	}

	private static Runnable createRunnable(LockFreeStringList2 list, int id) {
		return () -> {
			for (var j = 0; j < 10_000; j++) {
				list.addLast(id + " " + j);
			}
		};
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		var threadCount = 5;
		var list = new LockFreeStringList2();
		var tasks = IntStream.range(0, threadCount).mapToObj(id -> createRunnable(list, id)).map(Executors::callable)
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
 * VarHandle permet de récupérer par référence (& en C) Changer les valeurs
 * d'une case mémoire dans la RAM Besoin d'un seul VarHandle pour changer
 * plusieurs case si elles sont du même type.
 **/