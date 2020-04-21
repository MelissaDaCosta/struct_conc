Collections concurrentes - lab5
<br>
Single instruction, Multiple Data (SIMD) 
========================================================================

# Exercice 2 - Vectorized Add

Au lieu de prendre les élem un par un, prendre elem avec une certaine taille de vecteur.

Chaque CPU a des vecteur de taille différentes.
SPECIES = taille de vecteur qu'on va pouvoir avoir

## 2. Point communs et différences entre API Fork/Join et API des Vector :

point communs : parallélisation

dans les 2 cas = API parrallèle, but de faire calcul en parallèle

vecteur = calcul sur un meme coeur -> transformation calcul CPU
on croit manipuler des int mais en fait ça fait des trucs vodoo processeur  pour amelioer les operations (sur 1 thread)

fork/join = répartir calcul sur plusieurs coeurs

fork/join: repartir la tache sur plusiuers coeurs, SIMD faire plusieurs calculs sur 1 seul coeur
Single instruction, Multiple Data (SIMD)
un vecteur t'as plusieurs int dedans mais tu peut calculer le tout avec UNE instructeur
et pas X = nombre de data

1 IntVector = plusieurs int (SPECIES)
System.out.println(SPECIES);
System.out.println(SPECIES.length());
Affiche :
Species[int, 8, S_256_BIT]
8

1 IntVector = 8 int
donc "normalement" tu a besoin de 8 fois moins d'instructions

> On pourrait mêler les 2 mais en gros askip ton PC il fait croire t'as 2 coeurs virtuels chelou et le SIMD il aime pas



## 4. Dans la classe VectorizedBenchMark, dupliquer la méthode de benchmark pour benchmarker la méthode sumReduceLane. Lancer le benchmark, est-ce que le résultat est satifaisant (la réponse est non), pourquoi ? 


Voici les résultats  :
```
Benchmark                          Mode  Cnt    Score     Error  Units
VectorizedBenchMark.sumReduceLane  avgt    5  384,874 ± 108,696  us/op
VectorizedBenchMark.sum_loop       avgt    5  461,196 ± 656,489  us/op
```
 
sumReduceLane devrait être SPECIES fois plus rapide or il est juste "un peu" plus rapide que sumLoop
-> Ce qu'il faut faire c'est utiliser un vecteur à la place de la variable sum qui est juste un int !



LaneWise = 
demander a chaque morceau du vecteur de travailler pour faire le calcul en parallèlle.

=> petit soucis de perf :
Je pense qu'il y a de toute façon un problème avec l'API, pas qu'un problème de perturbation

# Exercice 3 - Vectorized Sub


##1. Quelle méthode de l'API de vectorisation n'est pas disponible pour l'opération -, pourquoi ?

reduceLanes prend pas SUB parce que elle est pas associative contrairement à ADD
 
