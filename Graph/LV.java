package Graph;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class LV {
    private GrapheDeControle<String> graphe;
    private Map<String, Set<String>> LVentry;
    private Map<String, Set<String>> LVexit;

    public LV(GrapheDeControle<String> graphe) {
        this.graphe = graphe;
        LVentry = new HashMap<>();
        LVexit = new HashMap<>();

        for (String sommet : graphe.vertices) {
            LVentry.put(sommet, new HashSet<>());
            LVexit.put(sommet, new HashSet<>());
        }
    }

    public void effectuerAnalyse() {
        boolean hasChanged;
        do {
            hasChanged = false;

            for (String sommet : graphe.vertices) {
                Set<String> oldLVexit = new HashSet<>(LVexit.get(sommet));

                // LVexit(B) = union de LVentry des successeurs de B
                Set<String> newLVexit = new HashSet<>();
                for (String successeur : graphe.getOutNeighbors(sommet)) {
                    newLVexit.addAll(LVentry.get(successeur));
                }
                LVexit.put(sommet, newLVexit);

                // LVentry(B) = (LVexit(B) - tué(B)) + généré(B)
                Set<String> newLVentry = new HashSet<>(newLVexit);
                newLVentry.removeAll(variablesDefiniesDans(sommet));
                newLVentry.addAll(variablesUtiliseesDans(sommet));
                LVentry.put(sommet, newLVentry);

                if (!oldLVexit.equals(newLVexit)) {
                    hasChanged = true;
                }
            }
        } while (hasChanged);
    }

    private Set<String> variablesUtiliseesDans(String instruction) {
        Set<String> utilisees = new HashSet<>();
        String[] parties = instruction.split("\\s+");

        // Les variables utilisées sont les registres après l'opération et le registre défini.
        for (int i = 2; i < parties.length; i++) {
            if (estUnNomDeRegistre(parties[i])) {
                utilisees.add(parties[i]);
            }
        }

        return utilisees;
    }

    private Set<String> variablesDefiniesDans(String instruction) {
        Set<String> definies = new HashSet<>();
        String[] parties = instruction.split("\\s+");

        // La variable définie est généralement le premier registre après l'opération
        if (parties.length > 1 && estUnNomDeRegistre(parties[1])) {
            definies.add(parties[1]);
        }

        return definies;
    }

    private boolean estUnNomDeRegistre(String element) {
        // Vérifier si l'élément correspond au motif d'un nom de registre
        return element.matches("R\\d+");
    }


    public Map<String, Set<String>> getLVentry() {
        return LVentry;
    }

    public Map<String, Set<String>> getLVexit() {
        return LVexit;
    }
    public void afficherAnalyse() {
        System.out.println("Analyse des Variables Vivantes:");
        for (String sommet : graphe.vertices) {
            System.out.println("Instruction: " + sommet);
            System.out.println("LVentry: " + LVentry.get(sommet));
            System.out.println("LVexit: " + LVexit.get(sommet));
            System.out.println();
        }
    }
}