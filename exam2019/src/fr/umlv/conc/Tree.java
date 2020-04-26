package fr.umlv.conc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tree<E extends Comparable<? super E>> {

  private static class ReduceTask<E extends Comparable<? super E>> extends RecursiveTask<E> {
    private BinaryOperator<E> op;
    private Node<E> tree;
    private int threshold;
    private E initialElement;

    ReduceTask(Node<E> tree, int threshold, E initialElement, BinaryOperator op) {
      this.tree = tree;
      this.op = op;
      this.threshold = threshold;
      this.initialElement = initialElement;
    }

    @Override
    protected E compute() {
      if (tree.size < threshold) {
        return tree.reduceSequential(initialElement, op);
      } else {
        var r1 = new ReduceTask<>(tree.left, threshold, initialElement, op);
        var r2 = new ReduceTask<>(tree.right, threshold, initialElement, op);
        r1.fork();
        var res2 = r2.compute();
        var res1 = r1.join();
        return op.apply(res1, res2);
      }
    }
  }

  static class Node<E extends Comparable<? super E>> {
    private Node<E> left;
    private Node<E> right;
    private E value;
    private int size;

    boolean add(E element) {
      if (value == null) {
        value = element;
        size++;
        return true;
      }
      if (element.equals(value)) {
        return false;
      }
      size++;
      if (element.compareTo(value) < 0) {
        if (left == null) {
          left = new Node<>();
        }
        return left.add(element);
      }
      if (right == null) {
        right = new Node<>();
      }
      return right.add(element);
    }

    void forEach(Consumer<? super E> consumer) {
      if (left != null) {
        left.forEach(consumer);
      }
      if (value != null) {
        consumer.accept(value);
      }
      if (right != null) {
        right.forEach(consumer);
      }
    }

    E reduceSequential(E initialElement, BinaryOperator<E> merger) {
      if (size == 0)
        return initialElement;

      // classe anonyme
      var box = new Object() {
        private E acc = initialElement;
      };

      forEach(n ->
      {
        box.acc = merger.apply(n, box.acc);
      });

      return box.acc;
    }

  }

  /***************************************************************************/

  private final Node<E> root = new Node<>();

  public boolean add(E element) {
    Objects.requireNonNull(element);
    return root.add(element);
  }

  public void forEach(Consumer<? super E> consumer) {
    Objects.requireNonNull(consumer);
    root.forEach(consumer);
  }

  @Override
  public String toString() {
    var joiner = new StringJoiner(", ", "[", "]");
    forEach(element -> joiner.add(element.toString()));
    return joiner.toString();
  }


  public E reduceSequential(E initialElement, BinaryOperator<E> merger) {
    Objects.requireNonNull(initialElement);
    Objects.requireNonNull(merger);
    return root.reduceSequential(initialElement, merger);
  }


  public E reduceParallel(int threshold, E initialElement, BinaryOperator<E> merger) {
    var pool = ForkJoinPool.commonPool();
    var task = new ReduceTask<>(root, threshold, initialElement, merger);
    return pool.invoke(task);

  }

  public static void main(String[] args) {
    var tree = new Tree<String>();
    tree.add("hello");
    tree.add("ben");
    tree.add("bob");
    tree.add("alice");

    System.out.println(tree);

    var treeI = new Tree<Integer>();

    var random = new Random(0);
    var list = random.ints(100, 0, 100).boxed().collect(Collectors.toList());
    Collections.shuffle(list);
    // shuffle pour que on ajoute un coup à droite et un coup à gauche
    list.forEach(treeI::add);
    System.out.println(treeI);


    var sum = treeI.reduceSequential(0, (a, b) -> a + b);
    System.out.println(sum);

    var sumParallel = treeI.reduceParallel(10_000, 0, (a,b)->a+b);
    System.out.println(sumParallel);

  }
}
