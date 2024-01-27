package Graph;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GrapheDeControle<T> extends OrientedGraph<T> {

    public GrapheDeControle() {
        super();
    }


    // création des sommets avec les labels
    public void ajouterSommets(List<T> instructions) {
        for (T instruction : instructions) {
            this.addVertex(instruction);
        }
    }

    // Vérifie si une instruction est une étiquette
    private boolean estUneEtiquette(String instruction) {
        return instruction.matches(".*:.*");
    }


    // Vérifie si une instruction est une instruction de saut
    private boolean estUneInstructionDeSaut(String instruction) {
        String[] mots = instruction.trim().split("\\s+");
        String opcode = mots[0];
        return opcode.equals("JSUP") || opcode.equals("JINF") || opcode.equals("JEQU") || opcode.equals("JMP");
    }

    private String getCibleDuSaut(String instruction) {
        String[] mots = instruction.trim().split("\\s+");
        return mots[mots.length - 1]; // La cible est le dernier mot de l'instruction
    }

    public void ajouteAretes(List<String> instructions) {
        Map<String, Integer> etiquettes = new HashMap<>();

        // Identifier les étiquettes et leurs positions
        for (int i = 0; i < instructions.size(); i++) {
            String instruction = instructions.get(i);
            if (estUneEtiquette(instruction)) {
                // Le nom de l'étiquette est extrait de l'instruction
                String nomEtiquette = instruction.substring(0, instruction.indexOf(":")).trim();
                // L'étiquette pointe directement vers l'instruction associée
                etiquettes.put(nomEtiquette, i);
            }
        }

        // Analyser et ajouter des arêtes pour les instructions de saut
        for (int i = 0; i < instructions.size(); i++) {
            String instructionActuelle = instructions.get(i);
            T sommetActuel = (T) instructionActuelle;

            // Ajouter une arête vers l'instruction suivante par défaut
            if (i < instructions.size() - 1) {
                T sommetSuivant = (T) instructions.get(i + 1);
                this.addEdge(sommetActuel, sommetSuivant);
            }

            // Ajouter des arêtes pour les instructions de saut
            if (estUneInstructionDeSaut(instructionActuelle)) {
                String cible = getCibleDuSaut(instructionActuelle).trim();
                if (etiquettes.containsKey(cible)) {
                    // La cible du saut pointe vers l'instruction associée à l'étiquette
                    int positionCible = etiquettes.get(cible);
                    T sommetCible = (T) instructions.get(positionCible);
                    this.addEdge(sommetActuel, sommetCible);
                }
            }
        }
    }
}