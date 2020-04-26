package fr.umlv.conc;

import java.util.OptionalDouble;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AverageVote {
  private final int party;  // champs final les threads voit le champs a la fin du constructeur
  private long sum;
  private int count;
  
  private ReentrantLock lock = new ReentrantLock();
  private Condition condition = lock.newCondition();
  
  public AverageVote(int party) {
    this.party = party;
  }
  
  public void vote(int value) throws InterruptedException {
    lock.lock();
    try {
      sum += value;
      count++;
      condition.signal();
    }finally {
      lock.unlock();
    }
  }
  
  public OptionalDouble average() {
    lock.lock();
    try{
      if (count == 0) {
        condition.signal();
        return OptionalDouble.empty();
      }
      condition.signal();
      return OptionalDouble.of(sum / (double)count);
      
    }finally {
      lock.unlock();
    }
  }
  
  public static void main(String[] args) {
    var vote = new AverageVote(5_000);
    new Thread(() -> {
      for (var start = System.currentTimeMillis(); System.currentTimeMillis() - start < 10_000;) {
        Thread.onSpinWait();
        // on spin wait pour ne pas faire de l'attente active
        // on interdit au thread d'être shédulé par l'os
        vote.average().ifPresent(average -> {
          if (average != 256.0) {
            throw new AssertionError("average " + average);
          }
          System.out.println("av = " + average);
        });
      }
      System.out.println("end !");
    }).start();
    
    
    for (var i = 0; i < 5_000; i++) {
      new Thread(() -> {
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
