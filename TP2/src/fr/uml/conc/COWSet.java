package fr.uml.conc;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import fr.uml.conc.LockFreeStringList2.Entry;

public class COWSet<E> {
	private final E[][] hashArray;
	// varhandle sur le tableau

	private static final Object[] EMPTY = new Object[0];

	static {
		var lookup = MethodHandles.lookup();
		try {
			NEXT_HANDLE = lookup.findVarHandle(Entry.class, "next", Entry.class);
			TAIL_HANDLE = lookup.findVarHandle(LockFreeStringList2.class, "tail", Entry.class);
		} catch (NoSuchFieldException | IllegalAccessException e) { // 2 cas qui ne devrait pas se produire
			throw new AssertionError(e); // pas censé être là
		}
	}

	@SuppressWarnings("unchecked")
	public COWSet(int capacity) {
		var array = new Object[capacity][]; // tableau de tableau
		Arrays.setAll(array, __ -> EMPTY); // tableau de tableau contiennent pas vide mais des tableau vide
		this.hashArray = (E[][]) array;
	}

	public boolean add(E element) {
		Objects.requireNonNull(element);
		var index = element.hashCode() % hashArray.length;
		// 1 lecture volatile
		for (var e : hashArray[index]) {
			if (element.equals(e)) { // si l'élément est déjà présent
				return false;
			}
		}
		// 1 lecture volatile
		var oldArray = hashArray[index];
		// agrandit de 1
		var newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
		// CAS
		newArray[oldArray.length] = element; // insère l'élément
		hashArray[index] = newArray; // assigne le tableau
		return true;
	}

	public void forEach(Consumer<? super E> consumer) {
		// varhandle.getVolatile(hashArray, index)
		for (var index = 0; index < hashArray.length; index++) {
			var oldArray = hashArray[index];
			for (var element : oldArray) {
				consumer.accept(element);
			}
		}
	}

	public static void main(String[] args) {

	}
}
