package Graph;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GrapheDeControle extends OrientedGraph<Integer> {
    // Cette carte stocke les étiquettes et leur indice associé dans la liste des instructions.
    private Map<String, Integer> etiquettes = new HashMap<>();

    public GrapheDeControle() {
        super();
    }

    public void ajouterSommets(List<String> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            addVertex(i); // Chaque instruction est représentée par son indice.
        }
    }

    public void ajouteAretes(List<String> instructions) {
        // Premièrement, identifier toutes les étiquettes et leur position.
        for (int i = 0; i < instructions.size(); i++) {
            if (estUneEtiquette(instructions.get(i))) {
                String nomEtiquette = getNomEtiquette(instructions.get(i));
                // Associer l'étiquette avec l'indice de l'instruction suivante dans la liste.
                etiquettes.put(nomEtiquette, i);
            }
        }

        // Deuxièmement, ajouter des arêtes pour les instructions normales et de saut.
        for (int i = 0; i < instructions.size(); i++) {
            if (!estStop(instructions.get(i))) {
                if (i < instructions.size() - 1 && !estUneEtiquette(instructions.get(i + 1))) {
                    this.addEdge(i, i + 1);
                }
                if (estUneInstructionDeSaut(instructions.get(i))) {
                    String cible = getCibleDuSaut(instructions.get(i));
                    if (etiquettes.containsKey(cible)) {
                        this.addEdge(i, etiquettes.get(cible));
                    }
                }
            }
        }
    }

    private boolean estUneEtiquette(String instruction) {
        // Vérifier si l'instruction contient une étiquette.
        return instruction.contains(":");
    }

    private String getNomEtiquette(String instruction) {
        // Obtenir le nom de l'étiquette à partir de l'instruction.
        int colonIndex = instruction.indexOf(':');
        if (colonIndex != -1) {
            return instruction.substring(0, colonIndex).trim();
        }
        return instruction; // Retourne toute l'instruction si aucune étiquette n'est trouvée.
    }

    private boolean estStop(String instruction) {
        return instruction.startsWith("STOP");
    }

    private boolean estUneInstructionDeSaut(String instruction) {
        return instruction.matches(".*(JSUP|JINF|JEQU|JMP|CALL|JNEQ|JIEQ|JSEQ).*");
    }

    private String getCibleDuSaut(String instruction) {
        String[] mots = instruction.split("\\s+");
        return mots[mots.length - 1];
    }
}