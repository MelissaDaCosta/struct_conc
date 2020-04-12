package fr.umlv.conc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import fr.umlv.conc.Reducer.ReduceTask;
import jdk.jfr.Threshold;

/**
 * Exercice 3 - ForkJoinCollections
 * 
 * @author melissa
 * 
 *         threshold = le seuil quand il est utile de diviser la tâche
 */

public class ForkJoinCollections {
  public static class ReduceTask<T, V> extends RecursiveTask {
    private Spliterator<T> spliterator;
    private V initialValue;
    private BiFunction<T, V, V> acc;
    private BinaryOperator<V> combiner;
    private int threshold;

    public ReduceTask(Spliterator<T> spliterator, int threshold, V initialValue,
        BiFunction<T, V, V> acc, BinaryOperator<V> combiner) {
      super();
      this.spliterator = spliterator;
      this.initialValue = initialValue;
      this.acc = acc;
      this.combiner = combiner;
      this.threshold = threshold;
    }

    @Override
    protected V compute() {
      if (spliterator.estimateSize() < threshold) { // assez petit pour faire le calcul direct
        return sequentialReduce(spliterator, initialValue, acc);
      } else {
        var spliterator2 = spliterator.trySplit();
        ReduceTask r1 = new ReduceTask(spliterator, threshold, initialValue, acc, combiner);
        ReduceTask r2 = new ReduceTask(spliterator2, threshold, initialValue, acc, combiner);
        r1.fork(); // déléguer cette partie a une autre thread
        var res2 = sequentialReduce(spliterator2, initialValue, acc); // continue de faire le calcul dans cette thread
        var res1 = r1.join(); // attend tant que r1 n'a pas fini son calcul
        return combiner.apply((V) res1, res2);
      }
    }
  }

  public static <V, T> V forkJoinReduce(Collection<T> collection, int threshold, V initialValue,
      BiFunction<T, V, V> accumulator, BinaryOperator<V> combiner) {

    return forkJoinReduce(collection.spliterator(), threshold, initialValue, accumulator, combiner);
  }

  private static <V, T> V forkJoinReduce(Spliterator<T> spliterator, int threshold, V initialValue,
      BiFunction<T, V, V> accumulator, BinaryOperator<V> combiner) {
    var pool = ForkJoinPool.commonPool();
    @SuppressWarnings("unchecked")
    var task = new ReduceTask<T, V>(spliterator, threshold, initialValue, accumulator, combiner);
    return (V) pool.invoke(task);
  }

  // Exercice 3
  public static <T, V> V sequentialReduce(Spliterator<T> spliterator, V initial,
      BiFunction<T, V, V> acc) {
    // var acc = initial;

    // class Box {
    // // champs mutable
    // private T acc = initial;
    // }
    // var box = new Box();

    // classe anonyme
    var box = new Object() {
      private V acc = initial;
    };

    // true tant qu'il y a des éléments dans le spliterator
    while (spliterator.tryAdvance(e ->
    {
      // acc = op.apply(acc, e);
      // a l'intérieur d'une lambda, on a pas le droit de mofifier la valeur d'une variable locale
      // -> on uitilise une classe Box pour accéder aux champs
      box.acc = acc.apply(e, box.acc);
    }));
    return box.acc;
  }

  public static void main(String[] args) {
    // sequential
    System.out.println(IntStream.range(0, 10_000).sum());

    // fork/join
    var list = IntStream.range(0, 10_000).boxed().collect(Collectors.toList());
    var result =
        forkJoinReduce(list, 1_000, 0, (acc, value) -> acc + value, (acc1, acc2) -> acc1 + acc2);
    System.out.println(result);
  }

}
