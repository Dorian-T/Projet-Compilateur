package Graph;
import java.util.*;

public class GrapheDeConflits extends UnorientedGraph<String> {
    private Map<String, Set<String>> tableauConflits; // Nouveau champ pour enregistrer les conflits

    public GrapheDeConflits(Map<String, Set<String>> LVexit) {
        tableauConflits = new HashMap<>();

        Set<String> variables = new HashSet<>();
        for (Set<String> vars : LVexit.values()) {
            variables.addAll(vars);
        }

        for (String var : variables) {
            this.addVertex(var);
            tableauConflits.put(var, new HashSet<>()); // Initialiser l'ensemble de conflits pour chaque variable
        }

        for (Set<String> vars : LVexit.values()) {
            for (String var1 : vars) {
                for (String var2 : vars) {
                    if (!var1.equals(var2)) {
                        this.addEdge(var1, var2);
                        tableauConflits.get(var1).add(var2); // Enregistrer le conflit
                    }
                }
            }
        }
    }

    public void afficherTableauConflits() {
        for (String var : tableauConflits.keySet()) {
            System.out.println("Variable " + var + " est en conflit avec " + tableauConflits.get(var));
        }
    }
}
