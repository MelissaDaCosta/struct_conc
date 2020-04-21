Collections concurrentes - TP4
========================================================================

# Exercice 1 - Rappel sur les fonctions d'ordre supérieur

## 1.

> intBinaryOperator
```
Represents an operation upon two int-valued operands 
and producing an int-valued result.
```

-----------------------------------------------------------------------------

# Exercice 2 - Grosse réduction

> La technique Fork/Join consiste à utiliser l'algorithme suivant

```
   solve(problem):
   if problem is small enough:
      solve problem directly (sequential algorithm)
   else:
      divide the problem in two parts (part1, part2)
      fork solve(part1)
      solve(part2)
      join part1
      return combined results
 ```

Java depuis la version 7 fourni les classes ForkJoinPool et RecursiveTask 
qui correspondent respectivement à un pool de thread ayant des tâches 
sachant se subdiviser et une tâche en elle-même. 

## 3.

L'idée de la question 3 est de faire la même chose que parallel mais à la main
parallel ça veut pas dire rapide : 
si tu passes plus de temps à distribuer le calcul et à le ré-agréger, c'est plus lent

-> Intéressant pour des opérations lourdes : si ton cacul est couteux, pas + ou max, alors ça vaut le cout

C'est pour cela que la parallélisation est pas automatique, tu le fait à la main !
tu decides si tu appeles .parallel() ou pas ! Ca dépend de ton problème

### ForkJoinPool

An ExecutorService for running ForkJoinTasks. 
A ForkJoinPool differs from other kinds of ExecutorService mainly by virtue of employing work-stealing: 
all threads in the pool attempt to find and execute tasks submitted to the pool 
and/or created by other active tasks (eventually blocking waiting for work if none exist).

Même chose qu'un ThreadPool sauf que capable de faire des join, capable de s'arrêter.

http://tutorials.jenkov.com/java-util-concurrent/java-fork-and-join-forkjoinpool.html

### 1. Pourquoi on ne peut pas utiliser un ThreadPoolExecutor classique comme piscine à threads mais un ForkJoinPool dans notre cas ?
> Qu'est ce que l'on doit jamais faire dans un Runnable/Callable d'un ThreadPoolExecutor ?

On ne doit pas faire un join sur le thread car join est bloquant, 
il attend que le thread soit terminé.
Si on fait des appels bloquants on peut arrêter toutes les threads du pool 
et on a un deadlock.
Un deadlock entre la sousmission d'une nouvelle tache qui attend qu'une thread soit dispo 
et toutes les threads en attente que la tache que l'on doit soumettre est fini.
Lorsque l'on fait un join() dans une RecursiveTask, on enlève la tache qui appel le join() 
du ForkJoinPool et on la remet lorsque la tache qui fait le calcul sur lequel on attend 
a fini son calcul comme cela pas de deadlock.

### 2. Comment obtenir les ForkJoinPool par défaut ?

On doit utiliser commonPool :
> static ForkJoinPool commonPool() : Returns the common pool instance.

### 3. Comment doit on envoyer notre tâche à exécuter (notre RecursiveTask) sachant que l'on veut récupérer la valeur de retour ?
Note: c'est la même méthode que pour le ThreadPoolExecutor classique ! 

On appelle `invoke` :
> <T> T invoke​(ForkJoinTask<T> task) : Performs the given task, returning its result upon completion.

Lorsque l'on est a l'extérieur et que l'on veut demander l'execution d'une tache récursive, on va utiliser invoke().
Comment savoir si le problème est petit : on regarde le nombre d'élément.

=> Plus de contrôle qu'un stream parallèle :
- contrôle sur le moment où on décise que c'est assez petit
- choisir exactement l'algo utilisé


--------------------------------------------------

# Exercice 3 - ForkJoinCollections 

Voir que ca marche avec n'importe qu'elle collections, pourvu qu'on soit capacble de couper cette 
collection en 2 (comme avec le tableau)
 
 > trySplit : couper le Spliterator en deux.
 
 Spliterator fourni 3 choses : 
 - estimateSize : quelle est la taille des données du spliterator
 - tryAdvance : parcourir les éléments
 - trySplit : demander de dupliquer les choses (peut renvoyer null s'il sais pas couper)
 
 
### 1. Quel sont les types XXX et YYY ou dit différemment pourquoi on a besoin de deux lambdas en paramètre de forkJoinReduce ? 

On a besoins d'un accumulateur pour additioner toutes les valeurs du spliterator.
C'est une BiFunction qui prend un T et un V et renvoie un V : BiFunction<T, V, V>

On a besoins d'un combineur qui permet de combiner les 2 résultats des 2 sous-tâches faites.
C'est un BinaryOperator qui prend un V et renvoie un V : BinaryOperator<V>

> La BiFunction permet de paramétrer avec des types plus précis

 ### 3. Que peut-on en conclure sur la façon dont les Stream parallèles sont implantés ? 

 L'API des Stream parallèles utilise la technique Fork/Join














