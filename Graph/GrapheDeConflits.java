package Graph;
import java.util.*;

public class GrapheDeConflits {
    private UnorientedGraph<Integer> grapheConflit;
    private HashMap<String, Set<Integer>> lvExitData;
    private Set<Integer> tousLesRegistres;

    public GrapheDeConflits(HashMap<String, Set<Integer>> lvExitData, Set<Integer> tousLesRegistres) {
        this.lvExitData = lvExitData;
        this.tousLesRegistres = tousLesRegistres;
        this.grapheConflit = new UnorientedGraph();
        construireGraphe();
    }

    private void construireGraphe() {
        // Ajouter tous les registres comme nœuds
        for (Integer reg : tousLesRegistres) {
            grapheConflit.addVertex(reg);
        }

        // Ajouter des arêtes pour les registres en conflit
        for (Set<Integer> variables : lvExitData.values()) {
            for (Integer var : variables) {
                for (Integer otherVar : variables) {
                    if (!var.equals(otherVar) && !grapheConflit.hasEdge(var, otherVar)) {
                        grapheConflit.addEdge(var, otherVar);
                    }
                }
            }
        }
    }

    public int colorerGraphe() {
        return grapheConflit.color();
    }

    public UnorientedGraph<Integer> getGrapheConflit() {
        return grapheConflit;
    }
}