lab3
<br> 
Memory Model, Publication et Lock 
======================================================

## Exercice 1 - SpinLock pas réentrant

1.Rappeler ce que "réentrant" veux dire. 

 C'est une exclusion mutuelle, un mécanisme de verrouillage récursif,
qui peut être bloqué plusieurs fois par le même processus ou fil 
(d'exécution) sans toutefois causer un interblocage.

=> Définir des sections critiques
Mécanisme de verrou (=mutex)
lock() et unclock()

Réentrant = Le fait qu'une fonction puisse se rappeler elle même
donc non ré-entrant c'est le fait que lorsque l'on est dans une fonction,
personne d'autre n'a le droit d'être dedans (pas une autre thread et pas la même thread)
dans le cas des locks, un lock non-reentrant est un lock que l'on peut prendre que une fois
Il existe des verrous réentrant et non réentrant (dans la vrai vie, toujours réentrant)

exemple :
* Thread A calls function F which acquires a reentrant lock for itself before proceeding
* Thread B calls function F which attempts to acquire a reentrant lock for itself but 
cannot due to one already outstanding, resulting in either a block (it waits), or a timeout if requested
* Thread A's F calls itself recursively. It already owns the lock, so it will not block itself (no deadlock). 
This is the central idea of a reentrant mutex, and is what makes it different from a regular lock.
* Thread B's F is still waiting, or has caught the timeout and worked around it
* Thread A's F finishes and releases its lock(s)
* Thread B's F can now acquire a reentrant lock and proceed if it was still waiting

-----------------------------------------------------------------------------

2. Expliquer, pour le code ci-dessous, quel est le comportement que l'on attend si la classe est thread-safe.

 Si la classe est thread safe, 2 threads sont lancé et pour chacun un conteur de 0 à 1M
 augmente.
 
 Le code du Runnable n'est pas thread safe même si counter est volatile.
 Car même si volatile, counter++ pas atomique !
 
 (AtomicInteger ca aurait passer mais trop lent donc VarHandle pour faire la même chose)
 
 Et comme les méthodes lock et unlock sont vides, ce n'est pas thread safe !
 
 --------------------------------------------------------------------------
 
 3.
 ```java
  public void lock() {
    // attendre tant que le lock est déjà pris -> attente active
    while (isLocked) {
      Thread.onSpinWait();// résout l'attente active -> ne veut pas que ce thread soit schédulé par
                          // l'os
    }	// peut être interrompue ici

    isLocked = true;

  }
  ```
  
  Pas thread safe car on peut être interrompue entre le moment ou on test et le moment ou on prend le lock
  => utilisé un compareAndSet pour essayer de faux à vrai
  => si plante : boucle qui attend
  
  PAS réentrant lock car si 2 fois lock :
  on est bloqué à vie
 
----------------------------------------------------------------------------------------------------

## Exercice 2 - SpinLock Réentrant

si plusieurs threads peuvent faire la même opération en même temps c'est un CAS !
si une seul thread peut faire l'op, alors pas besoin

unlock = un seul thread car il faut qu'il est fait le lock avant
mais pour lock il peut y avoir plusieurs threads

le CAS rend l'accès à une variable volatile -> atomique


2.

Le champ ownerthread peut ne pas être volatile -> gain de performances
RAPPEL : si écriture volatile : garantie que toutes les autres écritures sont fait en RAM avant
  
Ecriture volatile ~~ **force le CPU à s'arreter** donc avec un champ en moins :
beaucoup mieux
  
Si une thread ne voit pas qu'on a écrit -> pas une thread importante
Une thread peut voir que ownerthread vaut null (donc valeur pas encore écrite) 
ca va quand meme planter dans le test ownerThread!=currentThread
  
Note: la lecture d'un champ par une même thread ne nécessite pas de lecture/ecriture volatile. 

Pour la lecture, faut lire d'abbord un volatile
Pour l'écriture, faut écrire à la fin dans un volatile

---------------------------------------------------------------------------------------------------------------
 
 ## Exercice 3 - Double-Checked Locking
 
Le Double-Checked Locking est un design pattern de concurrence hérité du C++ 
qui consiste à essayer d'initialiser un singleton de façon paresseuse (lazy) 
et thread-safe.
Ce design pattern ne sert à rien en Java (on va voir pourquoi)
mais c'est un exercice comme un autre. 
 
> Si on initialise pas une classe, le champ static ne sera pas initialisé !

 ------------------------------------------------------------------------------------------
 
 1.
 Pourquoi le code suivant ne marche pas ?

```java
public class Utils {
  private static Path HOME;

  public static Path getHome() {
    if (HOME == null) {
      return HOME = Path.of(System.getenv("HOME"));
    }
    **peut-être dé-schédulé ici**
    return Home;
  }
}
```

System.getEnv est très lent -> va chercher variable d'environnement
Donc on veut le faire une seul fois

L'initialisé à null car si personne appelle getHome on veut pas l'initialisé.

===> Design pattern = singleton paresseux
singleton == global
singleton = accessible à n'importe qu'elle endroit du programme 
et correspond à une seule variable
parresseux car si on appelle pas -> pas initialisé

Le code suivant ne marche pas car 
On aurait 2 fois un appel à getHome -> singleton initialisé 2 fois **OR**
le principe d'un singleton est d'être initialisé 1 fois. 
Donc ca ne marche pas car ca ne respecte pas le "protocole" du pattern.

---------------------------------------------

2. Peux-t'on dire que le code ci-dessus n'est pas thread-safe ? 

Le code n'est pas thread safe car on peut être dé-schédulé dans le if et avant le return HOME = Path...
Si on initialise dans un bloc static -> devient thread safe mais ce sera plus lazy.
Donc ca ne marche pas car ca ne respecte pas le "protocole" du pattern.

Correction du problème :

```java
public class Utils {
  private static Path HOME;
  private final static Object lock = new Object();

  public static Path getHome() {
	  synchronized(lock){
	    if (HOME == null) {
	      return HOME = Path.of(System.getenv("HOME"));
	    }
	    return Home;
	  }
  }
}
```

=> A chaque fois que l'on appelle getHome, on peut être bloqué sur le lock.
Donc au lieu de faire le synchronized à chaque fois, essayer à l'intérieur du if
(code question 3)

------------------------------------------------------

3. Pourquoi le code suivant ne marche pas non plus ?
Indice: pensez au problème de publication !
Comment peut-on corriger le problème ?

```java
public class Utils {
  private static Path HOME;
  private final static Object lock = new Object();

  public static Path getHome() {
    if (HOME == null) {
        synchronized(Utils.class) {
        if (HOME == null) {
          return HOME = Path.of(System.getenv("HOME"));
        }
      }
    }
    return HOME;
  }
}
```

Si synchronized(Utils.class) pb si quelqun d'autre fait
un synchronized(Utils.class) => deadlock

Si pas de 2ème if dans le bloc synchronized,
on peut se faire dé-schédulé avant de rentrer dans le bloc synchronized
=> donc rajouter un if aussi dans le bloc synchronized ==> double check

Ce code ne fonctionne pas, il y a un problème de publication.
Pb de publication : on voit les objets pas fini d'être initialisés.
On peut voir le path créee par le Path.of pas finit d'être initialisé.

Un 1er thread qui rentre : créer le Path.of (fait tout un tas d'opérations)
N'a pas eu le temps d'initialisé tous les champs de Path
On écrit dans HOME alors qu'on a pas finit d'initialiser tous les champs avec Path.of
Un autre thread rentre et return HOME car passe pas dans le if car HOME != null
mais HOME = caca 
La 1ère thread bloquée
**Si on lit une variable en dehors de synchronized on peut lire un champ pas finit 
d'être initialisé. Ca marche mais après plein de NPE**
==> apporte des NUllPointerException    

Si on met HOME volatile ca résout tous les problèmes !

car écriture de HOME dans volatile garantie que toutes les écritures
de Path sont faites avant.
+ garantie que toutes les lectures faites après sont rechargées à partir de la RAM

------------------------------------------------------
 
4.
 
Au lieu de faire des lectures et écritures volatiles : barrière mémoire + perf
grâce à getAcquire (=getVolatile) coute moins cher que getVolatile
setRelease
 
Au lieu d'avoir le champ volatile : on utilise getAcquire et setRelease
 