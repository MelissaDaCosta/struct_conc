package fr.umlv.conc;

/**
 * Exercice 1 - A vos chronometres
 * 1
 * @author melissa
 * Code à la base sans les synchronized
 */
public class Bogus {
  private boolean stop;
  
  private final Object lock = new Object();
  //63 221 764

  public void runCounter() {
		  var localCounter = 0;
		  for(;;) {
			  // a chaque tour de boucle, on force à prendre le jeton
			  // force a ce que la lecture soit faire en RAM
			  synchronized(lock){
			  if (stop) {
				  break;
			  }
			}
			  localCounter++;
		  }
		  System.out.println(localCounter);
  }

  public void stop() {
	  // force l'ecriture en RAM
	  synchronized(lock) {
		  stop = true;
	  
	  }
  }

  public static void main(String[] args) throws InterruptedException {
    var bogus = new Bogus();
    var thread = new Thread(bogus::runCounter);
    thread.start();
    Thread.sleep(100);
    bogus.stop();
    thread.join();
  }
}


/**
SANS LA CORRECTION AVEC LES SYNCHRONIZED :
 
La data race entre les threads est stop
Le programme lance un thread qui augmente un compteur et ensuite 
demande au thread de se mettre en pause (Thread.sleep) 
Puis, il modifie la variable stop
Puis, le programme attend que le thread meurt avec la méthode join.
La méthode static stop s'excute sur le thread courant.


Il ya 2 thread : le principale et le new Thread

Le programme est infinie car il crée une var locale stop et donc la modification
dans la méthode stop ne se fait pas
L'opti met la var stop dans un registre car + rapide
et la méthode stop fait la modif dans la ram
*/