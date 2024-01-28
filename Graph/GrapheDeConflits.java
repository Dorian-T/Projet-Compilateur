package Graph;
import java.util.*;

public class GrapheDeConflits{
    private UnorientedGraph<Integer> grapheConflit;
    private HashMap<String, Set<Integer>> lvExitData;

    public GrapheDeConflits(HashMap<String, Set<Integer>> lvExitData) {
        this.lvExitData = lvExitData;
        this.grapheConflit = new UnorientedGraph<Integer>();
        construireGraphe();
    }

    private void construireGraphe() {
        for (String instr : lvExitData.keySet()) {
            Set<Integer> variables = lvExitData.get(instr);

            // Assure-toi que R0 est inclus si nécessaire
            variables.add(0); // Ajoute R0

            for (Integer var : variables) {
                grapheConflit.addVertex(var);
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




