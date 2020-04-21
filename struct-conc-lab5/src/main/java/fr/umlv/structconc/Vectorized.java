package fr.umlv.structconc;

import java.util.function.IntBinaryOperator;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/*
 * Pour lancer : java --add-modules jdk.incubator.vector -jar target/benchmarks.jar
 */
public class Vectorized {
  public static int sumLoop(int[] array) {
    var sum = 0;
    for (var value : array) {
      sum += value;
    }
    return sum;
  }

  /**
   * Exercice 2 - Vectorized Add
   */

  // taille préféree
  // regarde dans le CPU la taille en nbr de int que l'on peut avoir
  // MAX peut faire chauffer le CPU => PREFERRED
  private static VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

  public static int sumReduceLane(int[] array) {
    var sum = 0;
    var i = 0;
    var limit = array.length - (array.length % SPECIES.length());
    // main loop
    // parcours le tableau SPECIES par SPECIES
    for (; i < limit; i += SPECIES.length()) {
      // utiliser les vecteurs
      // remplir un vecteur avec la partie du tableau lu
      IntVector vector = IntVector.fromArray(SPECIES, array, i);
      // prend tous les elem du vecteur et les additionne
      // somme partielle
      // cette opération ne fait pas une boucle, elle est faite automatiquement par le CPU !
      int oneSumPart = vector.reduceLanes(VectorOperators.ADD);
      sum += oneSumPart;

    }
    // post loop
    // si la taille du tableau n'est pas un multiple de SPECIES, il va rester des éléments dans le
    // tableau on utilise donc cette post loop
    // on réutilise le meme i que le for précédent
    for (; i < array.length; i++) {
      // pas utiliser les vecteurs
      // somme les élem qui sont rester car pas multiple de SPECIES
      sum += array[i];
    }
    return sum;
  }

  public static int sumLanewise(int array[]) {
    var i = 0;
    var limit = array.length - (array.length % SPECIES.length());
    // main loop
    var resultVector = IntVector.zero(SPECIES); // faire les additions dans ce vecteur là
    for (; i < limit; i += SPECIES.length()) {
      IntVector vector2 = IntVector.fromArray(SPECIES, array, i);
      // add les 2 vecteurs
      // opération add en lanewsise
      resultVector = resultVector.add(vector2);
    }
    // fait la somme des valeurs du vecteur
    var sum = resultVector.reduceLanes(VectorOperators.ADD);
    // post loop
    for (; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }

  /**
   * Exercice 3 - Vectorized Sub
   */

  public static int differenceLanewise(int array[]) {
    
    if(array.length == 0)
      return 0;
    
    var i = 0;
    var limit = array.length - (array.length % SPECIES.length());
    // main loop
    var resultVector = IntVector.zero(SPECIES); // faire les additions dans ce vecteur là
    for (; i < limit; i += SPECIES.length()) {
      IntVector vector2 = IntVector.fromArray(SPECIES, array, i);
      // add les 2 vecteurs
      // opération add en lanewsise
      //vec = vec.lanewise(ADD, tmp);
      resultVector = resultVector.add(vector2);
    }
    // fait la somme des valeurs du vecteur
    var sum = resultVector.reduceLanes(VectorOperators.ADD);
    // post loop
    for (; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }
}

