package fr.umlv.conc;

// 243 763 612, en faite ca dépend du coup, il s'arrête quand le sleep est fini

/**
 * Exercice 1 - A vos chronometres
 * 2
 * @author melissa
 *
 */
public class BogusVolatile {
	// On ne veut pas que ce champ soit dans un registre
  private volatile boolean stop;	
  
  //private final Object lock = new Object();
  

  public void runCounter() {
		  var localCounter = 0;
		  for(;;) {
			  // a chaque tour de boucle, on force à prendre le jeton
			  // force a ce que la lecture soit faire en RAM
			  //synchronized(lock){
			  if (stop) {
				  break;
			  //}
			}
			  localCounter++;
		  }
		  System.out.println(localCounter);
  }

  public void stop() {
	  // force l'ecriture en RAM
	  //synchronized(lock) {
		  stop = true;
	  
	  //}
  }

  public static void main(String[] args) throws InterruptedException {
    var bogus = new BogusVolatile();
    var thread = new Thread(bogus::runCounter);
    thread.start();
    Thread.sleep(100);
    bogus.stop();
    thread.join();
  }
}

/**
Les instructions assembleur sont atomiques
IL est possible d'accéder à ces opérations atomic grâce au package atomic

 Mot clé volatile = non pas dans un registre
 mais dans RAM (champs plus lent)
 moins puissant que synchronized
 
 AtomicInteger = nouvel object = pb de perf
 
 On ne sais pas si les opérations atomiques sont dispo sur notre architecture
 Avec compareAndSet on peut simuler l'opération
 CAS(&field, expectedValue, newValue)->boolean 
 
 On apelle les implantations qui n'ont ni blocs synchronized, ni lock : 
 lock-free
 
 On a la garantit avec volatile qu'on peut lire et écrire des 64 bits de facon atomique 
 (pas 32bit puis 32bits) 
 
 */
