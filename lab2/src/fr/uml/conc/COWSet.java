package fr.uml.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.print.attribute.HashAttributeSet;
import fr.uml.conc.LockFreeStringList2.Entry;

/**
 * Exercice 3 - Set 'Copy on Write'
 * 
 * @author melissa
 *
 * @param <E>
 */
public class COWSet<E> {
  
  private static int SIZE = 200_000;
  
  private final E[][] hashArray;

  private static final Object[] EMPTY = new Object[0];

  private static final VarHandle HASH_ARRAY_HANDLE;

  static {
    var lookup = MethodHandles.lookup();
    //HASH_ARRAY_HANDLE = lookup.findVarHandle(COWSet.class, "hashArray", Object[][].class);
    // récupère le hashArray
    HASH_ARRAY_HANDLE = MethodHandles.arrayElementVarHandle(Object[][].class);
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
    while (true) {
      // 1 lecture volatile
      var oldArray = (E[]) HASH_ARRAY_HANDLE.getVolatile(hashArray, index);
      // les lectures suivantes de oldArray sont faites comme si oldArray était un champ volatile
      // donc en RAM
      // oldArray = oldArray[index];
      for (var e : hashArray[index]) { // <-- 1 seul lect volat
        if (element.equals(e)) {
          return false;
        }
      }

      // agrandit de 1
      var newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
      newArray[oldArray.length] = element;
      // avant: hashArray[index] = newArray; // <- CAS ici
      // insère dans hashArray[index] newArray si hashArray[index]==oldArray
      if (HASH_ARRAY_HANDLE.compareAndSet(hashArray, index, oldArray, newArray))
        return true;
    }
  }

  public void forEach(Consumer<? super E> consumer) {
    for (var index = 0; index < hashArray.length; index++) {
      // var oldArray = hashArray[index];
      var oldArray = (E[]) HASH_ARRAY_HANDLE.getVolatile(hashArray, index);
      // les lectures suivantes de oldArray sont faites comme si oldArray était un champ volatile
      // donc en RAM
      for (var element : oldArray) {
        consumer.accept(element);
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    var set = new COWSet<Integer>(SIZE/10);

    var t1 = new Thread(() ->{
      for (var i = 0; i < SIZE; i++) {
        set.add(i);
      }
    });

    t1.start();

    var t2 = new Thread(() ->{
      for (var i = 0; i < SIZE; i++) {
        set.add(i);
      }
    });

    t2.start();

    t1.join();
    t2.join();
    
    var list = new ArrayList<>();
    
    // set.forEach(list::add);
    set.forEach((elem) ->{
      list.add(elem);
    });
    
    
    if(list.stream().count() == SIZE) {
      System.out.println("Only distinct elements");
    }else {
      System.out.println("Not only distinct elements");
    }
  }
}

/*
 * Classe pas thread safe car s'il y a plusieurs threads qui lance add, la data race est le tableau.
 * On peut lire des valeurs alors que les écritures précédentes n'ont pas encore été faites.
 * 
 * L'intérêt de la boucle while(true) dans la méthode add :
 * on prend un exemple 
 * avant: [3, 6] 
 * 2 threads t1 et t2
 * 
 * t1 veut inserer [3, 6, 9] 
 * t2 veut inserer [3, 6, 12]
 * 
 * t1 insère le tableau
  t2 fait le CaS -> oldArray([3, 6]) est different de current([3, 6, 9])
  donc t2 n'insère pas 12
  on refait un tour de boucle while(true)
  puis oldArray([3, 6, 9]) est == à current([3, 6, 9]) donc t2 fait le cAs et insère 12
  => la boucle while(true) règle le soucis sinon pas de nouvelle insertion concurrente
 */
