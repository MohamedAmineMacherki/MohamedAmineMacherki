# Monte Carlo Tree Search Planner - PDDL4J

## Description

Ce projet implémente un planificateur MCTS (Monte Carlo Tree Search) pour PDDL4J, utilisant des marches aléatoires pures pour résoudre des problèmes de planification automatique.

**Étudiants :** StrongAmineMohamed  
**Repository :** https://github.com/StrongAmineMohamed/mcts-pddl4j-planner

## Fonctionnalités

### Exercice 1 - Implémentation de base
- ✅ Planificateur MCTS avec marches aléatoires pures
- ✅ Intégration avec PDDL4J
- ✅ Comparaison avec HSP (A* planner)
- ✅ Tests sur 4 domaines : blocksworld, depot, gripper, logistics
- ✅ Métriques : temps d'exécution et longueur de plan
- ✅ Génération automatique de graphiques de comparaison

### Exercice 2 - Améliorations
- ✅ Adaptation de la longueur et nombre de marches aléatoires
- ✅ Monte-Carlo Deadlock Avoidance
- ✅ Monte-Carlo with Helpful Actions
- ✅ Optimisation des performances

## Installation et Compilation

### Prérequis
- Java 11 ou supérieur
- Maven 3.6+
- Python 3.7+ (pour les scripts de benchmark)
- PDDL4J 4.0.0

### Compilation
```bash
git clone https://github.com/StrongAmineMohamed/mcts-pddl4j-planner.git
cd mcts-pddl4j-planner
mvn clean compile package
```

## Utilisation

### Exécution du planificateur MCTS
```bash
java -cp target/classes:path/to/pddl4j.jar \
  fr.uga.pddl4j.planners.mcts.MCTSPlanner \
  -d domain.pddl \
  -p problem.pddl \
  -w 1000 \
  -l 100
```

### Comparaison avec HSP
```bash
python3 scripts/benchmark_comparison.py \
  --pddl4j-path path/to/pddl4j.jar \
  --domains-path pddl/
```

## Paramètres du Planificateur

| Paramètre | Description | Défaut |
|-----------|-------------|--------|
| `-w, --walks` | Nombre de marches aléatoires | 1000 |
| `-l, --length` | Longueur maximale des marches | 100 |
| `-d, --deadlock` | Activer l'évitement de deadlock | false |
| `-h, --helpful` | Utiliser les actions utiles | false |

## Algorithme MCTS

### Marche Aléatoire Pure
1. Partir de l'état initial
2. À chaque étape :
   - Vérifier si le but est atteint
   - Obtenir les actions applicables
   - Sélectionner une action aléatoire
   - Appliquer l'action
3. Répéter jusqu'au but ou longueur max

## Résultats de Benchmark

Les graphiques générés montrent la performance comparative entre HSP et MCTS sur différents domaines PDDL.

## Contact

**Auteur :** StrongAmineMohamed  
**Repository :** https://github.com/StrongAmineMohamed/mcts-pddl4j-planner  
**Date :** Juin 2025
