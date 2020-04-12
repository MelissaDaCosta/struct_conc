package fr.umlv.conc;

import java.util.Arrays;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.stream.Stream;

public class Reducer {

  // Exercice 2 - 3.3
  // classe paramétrée par le type de valeur de retour : Integer
  public static class ReduceTask extends RecursiveTask<Integer> {
    // subdiviser le tableau jusqu'à ce qu'on ai des problèmes plus simple.
    private final static int SIZE_LIMIT = 1024;
    private int start; // début du tableau
    private int end; // fin du tableau
    private int[] array; // le tableau
    private int initial;
    private IntBinaryOperator op;

    ReduceTask(int start, int end, int[] array, int initial, IntBinaryOperator op) {
      this.array = array;
      this.start = start;
      this.end = end;
      this.initial = initial;
      this.op = op;
    }

    @Override
    protected Integer compute() {
      if (end - start < SIZE_LIMIT) { // assez petit pour faire le calcul direct
        return Arrays.stream(array, start, end).reduce(initial, op);
      } else {
        var middle = (start + end) / 2;
        ReduceTask r1 = new ReduceTask(start, middle, array, initial, op);
        ReduceTask r2 = new ReduceTask(middle, end, array, initial, op);
        r1.fork(); // déléguer cette partie a une autre thread
        var res2 = r2.compute(); // continue de faire le calcul dans cette thread
        var res1 = r1.join(); // attend tant que r1 n'a pas fini son calcul
        return op.applyAsInt(res1, res2);
      }
    }
  }

  public static int sum(int[] array) {
    // var sum = 0;
    // for(var value: array) {
    // sum += value;
    // }
    // return sum;

    // return reduce(array, 0, Math::addExact);
    // return reduce(array, 0, Integer::sum);
    // return reduceWithStream(array, 0, Integer::sum);

    // utilise l'élement neutre, donc 0
    return parallelReduceWithStream(array, 0, Integer::sum);
  }

  public static int max(int[] array) {
    // var max = Integer.MIN_VALUE;
    // for(var value: array) {
    // max = Math.max(max, value);
    // }
    // return max;
    //
    // return reduce(array, Integer.MIN_VALUE, Math::max);
    // return reduceWithStream(array, Integer.MIN_VALUE, Math::max);

    // max a pas d'élément neutre
    return parallelReduceWithStream(array, Integer.MIN_VALUE, Math::max);
  }

  // Exercice 1
  public static int reduce(int[] array, int initial, IntBinaryOperator op) {
    var acc = initial;
    for (var value : array) {
      acc = op.applyAsInt(acc, value);
    }
    return acc;
  }

  // Exercice 2 - 1
  public static int reduceWithStream(int[] array, int initial, IntBinaryOperator op) {
    return Arrays.stream(array).reduce(initial, op);
  }

  // Exercice 2 - 2
  public static int parallelReduceWithStream(int[] array, int initial, IntBinaryOperator op) {
    return Arrays.stream(array).parallel().reduce(initial, op);
  }

  // Exercice 2 - 3.5
  public static int parallelReduceWithForkJoin(int[] array, int initial, IntBinaryOperator op) {
    var pool = ForkJoinPool.commonPool();
    var task = new ReduceTask(0, array.length, array, initial, op);
    return pool.invoke(task);
  }


  public static void main(String[] args) {
    // IntBinaryOperator max = Math::max;
    // IntBinaryOperator add = Integer::sum;

    // pour avoir 2 tableau pareil, fixer la seed (graine)
    var random = new Random(0);
    // générateur pseudo aléatoire
    // garantie qu'on ne trouve pas un cycle dans les valeurs
    // nbArg, min (compris, max(non compris)
    var array = random.ints(1_000_000, 0, 1_000).toArray();
    System.out.println(sum(array));
    System.out.println(max(array));
    System.out.println(parallelReduceWithForkJoin(array, 0, Integer::sum));
    System.out.println(parallelReduceWithForkJoin(array, Integer.MIN_VALUE, Math::max));
  }
}
