package Graph;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {

        //QUESTION1
        String file = "Graph/code_assembleur.txt";
        List<String> instructions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                instructions.add(ligne.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(instructions);
        GrapheDeControle<String> graphe = new GrapheDeControle();
        graphe.ajouterSommets(instructions);
        graphe.ajouteAretes(instructions);

        System.out.println();
        for (String instruction : instructions) {
            ArrayList<String> outNeighbors = graphe.getOutNeighbors(instruction);
            ArrayList<String> inNeighbors = graphe.getInNeighbors(instruction);
            System.out.println(instruction);
            System.out.println("Voisins sortants: " + outNeighbors);
            System.out.println("Voisins entrants: " + inNeighbors);
            System.out.println();
        }

        System.out.println("\n" + 
                "\n" + 
                "----------------------------------------- \n" + 
                        "\n");

        // QUESTION 2
        Variables variables = new Variables(graphe, instructions);
        variables.calculateLVEntryExit();

        for (String instr : instructions) {
            System.out.println("Instruction: " + instr);
            System.out.println("Variables générées: " + variables.getGeneratedVariables(instr));
            System.out.println("Variables tuées: " + variables.getKilledVariables(instr));
            System.out.println("LVEntry: " + variables.getLVEntry(instr));
            System.out.println("LVExit: " + variables.getLVExit(instr));
            System.out.println();
        }
        

        System.out.println("\n" + 
                "\n" + 
                "----------------------------------------- \n" + 
                        "\n");

        // QUESTION 3
        LV analyse = new LV(graphe);
        analyse.effectuerAnalyse();
        GrapheDeConflits grapheDeConflits = new GrapheDeConflits(analyse.getLVexit());
        grapheDeConflits.afficherTableauConflits();

        System.out.println("\n" + 
                "\n" + 
                "----------------------------------------- \n" + 
                        "\n");

        // QUESTION 4
        int nombreDeCouleurs = grapheDeConflits.color();
        System.out.println("Nombre de couleurs utilisées : " + nombreDeCouleurs);
    }
}