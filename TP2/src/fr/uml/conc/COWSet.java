package fr.uml.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import javax.print.attribute.HashAttributeSet;
import fr.uml.conc.LockFreeStringList2.Entry;

public class COWSet<E> {
  private final E[][] hashArray;

  private static final Object[] EMPTY = new Object[0];

  private static final VarHandle HASH_ARRAY_HANDLE;

  static {
    var lookup = MethodHandles.lookup();
    try {
      HASH_ARRAY_HANDLE = lookup.findVarHandle(COWSet.class, "hashArray", Object[][].class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);  // pas censé être là
    }
  }

  @SuppressWarnings("unchecked")
  public COWSet(int capacity) {
    var array = new Object[capacity][]; // tableau de tableau
    Arrays.setAll(array, __ -> EMPTY); // tableau de tableau contiennent pas vide mais des tableau
                                       // vide
    this.hashArray = (E[][]) array;
  }

  public boolean add(E element) {
    Objects.requireNonNull(element);
    var index = element.hashCode() % hashArray.length;
    // 1 lecture volatile
    var oldArray = (E) HASH_ARRAY_HANDLE.getVolatile(hashArray[index]);
    //oldArray = oldArray[index];
    for (var i=0; i<((String) oldArray).length(); i++) {
      if (element.equals(oldArray[i])) { // si l'élément est déjà présent
        return false;
      }
    }
    // agrandit de 1
    var newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
    // CAS
    HASH_ARRAY_HANDLE.compareAndSet(oldArray[index], newArray, newArray);
    //newArray[oldArray.length] = element; // insère l'élément
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
    new Thread(() ->
    {
      var set = new COWSet<>(200_000);

      for (var i = 0; i < 200_000; i++) {
        set.add(i);
      }
    }).start();

    new Thread(() ->
    {
      var set = new COWSet<>(200_000);

      for (var i = 0; i < 200_000; i++) {
        set.add(i);
      }
    }).start();


  }

}
