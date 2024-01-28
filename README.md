# Compilateur TCL

## Groupe 1 : Inférence de types

## Groupe 2 : Génération de Code

### Contributeurs

- SYLVESTRE ANTONIN
- VITTORE Thomas
- Ginhac Jules

### Aperçu

Le but de cette section est de générer un objet Program à partir de l’AST et des types obtenus par le groupe 1, en implémentant les méthodes de CodeGenerator.java. Cette classe contient des visiteurs de l’AST qui renvoient un objet de type Program, contenant un code linéaire, c’est-à-dire un code assembleur qui utilise un nombre arbitraire de registres.

### Architecture

La classe `CodeGenerator` étend `AbstractParseTreeVisitor` et implémente `grammarTCLVisitor`. Elle est responsable de parcourir l'arbre syntaxique et de générer le code assembleur correspondant.

### Fonctionnalités Principales

- **Opérations Arithmétiques et Logiques** : Implémente des méthodes pour générer du code assembleur pour des opérations telles que l'addition, la soustraction, la multiplication, la division et les opérations logiques (ET, OU).
- **Structures de Contrôle** : Comprend la génération de code pour les instructions if-else, les boucles while et for.
- **Gestion des Fonctions** : Gère les appels de fonctions, les arguments et les valeurs de retour.
- **Opérations sur les Tableaux** : Prend en charge l'accès et la manipulation des tableaux.

## Groupe 3 : Allocation de registres

### Contributeurs

- LARZUL JULIEN
- LAZRAK Salim
- MARTIN Albin

### Aperçu

Le but de cette section est de générer, à partir du code assembleur du groupe 2, un code assembleur simplifié ou le nombre de registre est diminué.

### Architecture

`Graph` - `UnorientedGraph` - `OrientedGraph`

### Fonctionnalités Principales

- **Graphe de contrôle** : Crée le graphe de contrôle à partir du code assembleur donné par le groupe 2
- **LVentry et LVexit** : Génère les LVentry et LVexit de chaque instruction du code à partir des variables générées et tuées du code assembleur. 
- **Graphe de conflit** : Création du graphe de conflit en fonction de nos valeurs de LVexit.
- **Coloration de graphe** : Coloration du graphe qui nous permet de voir combien de registres sont nécessaires.
- **Génération nouveau code assembleur** : Reécriture du code assembleur en éliminant les registres inutiles.

---
