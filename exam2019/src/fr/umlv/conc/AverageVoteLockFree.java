package fr.umlv.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AverageVoteLockFree {

  // lock free : ni block synchronized ni lock

  private static class Values {
    private final AtomicInteger count;
    private final AtomicLong sum;

    public Values() {
      this.count = new AtomicInteger();
      this.sum = new AtomicLong();
    }

    public void setSum(int value) {
      var currentValueSum = sum.get();
      var currentValueCount = count.get();
      if (sum.compareAndSet(currentValueSum, currentValueSum + value)
          && count.compareAndSet(currentValueCount, currentValueCount + 1))
        return;

    }

    OptionalDouble getAverage() {
      if (count.get() == 0) {
        return OptionalDouble.empty();
      }
      return OptionalDouble.of(Math.floor(sum.get() / (double) count.get()));

    }
  }

  private final int party; // champs final les threads voit le champs a la fin du constructeur
  private final Values values;

  public AverageVoteLockFree(int party) {
    this.party = party;
    this.values = new Values();
  }

  public void vote(int value) throws InterruptedException {
    values.setSum(value);
  }

  public OptionalDouble average() {
    return values.getAverage();

  }

  public static void main(String[] args) {
    var vote = new AverageVoteLockFree(5_000);
    new Thread(() ->
    {
      for (var start = System.currentTimeMillis(); System.currentTimeMillis() - start < 10_000;) {
        Thread.onSpinWait();
        System.out.println("av = " + vote.average());
        vote.average().ifPresent(average ->
        {
          if (average != 256.0) {
            throw new AssertionError("average " + average);
          }
        });
      }
      System.out.println("end !");
    }).start();


    for (var i = 0; i < 5_000; i++) {
      new Thread(() ->
      {
        try {
          vote.vote(256);
        } catch (InterruptedException e) {
          System.out.println("end of average");
          return;

        }
        System.out.println(vote.average().orElseThrow());
      }).start();
    }
  }
}
