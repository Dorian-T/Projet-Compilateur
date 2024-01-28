package Graph;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        //QUESTION1
        String file = "entrees.txt";
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

        // QUESTION 2
        System.out.println("\n-----------------------------------------\n");

        // QUESTION 3
        // Analyse des variables vivantes
        Variables variables = new Variables(graphe, instructions);
        variables.calculateLVEntryExit();

        // Construire un HashMap pour LVExit pour chaque instruction
        HashMap<String, Set<Integer>> lvExitMap = new HashMap<>();
        for (String instr : instructions) {
            Set<Integer> lvExitSet = variables.getLVExit(instr);
            lvExitMap.put(instr, lvExitSet);
        }

        // Afficher les résultats de l'analyse des variables vivantes
        for (String instr : instructions) {
            System.out.println("Instruction: " + instr);
            System.out.println("Variables générées: " + variables.getGeneratedVariables(instr));
            System.out.println("Variables tuées: " + variables.getKilledVariables(instr));
            System.out.println("LVEntry: " + variables.getLVEntry(instr));
            System.out.println("LVExit: " + variables.getLVExit(instr));
            System.out.println();
        }

        System.out.println("\n-----------------------------------------\n");

        // Construire le Graphe de Conflit
        GrapheDeConflits grapheDeConflit = new GrapheDeConflits(lvExitMap);
        UnorientedGraph<Integer> grapheConflit = grapheDeConflit.getGrapheConflit();

        // Afficher le graphe de conflit
        System.out.println("Graphe de Conflit:\n" + grapheConflit);

        // Appliquer la coloration du graphe de conflit
        int nombreDeCouleurs = grapheDeConflit.colorerGraphe();
        System.out.println("Nombre de couleurs utilisées pour la coloration: " + nombreDeCouleurs);
        // Obtenir la coloration pour chaque registre
        HashMap<Integer, Integer> colorationRegistres = new HashMap<>();
        for (int i = 0; i < nombreDeCouleurs; i++) { // Supposons que les registres sont numérotés de 0 à nombreDeCouleurs-1
            colorationRegistres.put(i, grapheConflit.getColor(i));
        }

        // Lire et réécrire le fichier 'exemple.txt'
        String fichierModifie = "sortie.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(file));
             BufferedWriter bw = new BufferedWriter(new FileWriter(fichierModifie))) {

            String ligne;
            while ((ligne = br.readLine()) != null) {
                for (Map.Entry<Integer, Integer> entry : colorationRegistres.entrySet()) {
                    ligne = ligne.replaceAll("R" + entry.getKey(), "R" + entry.getValue());
                }
                bw.write(ligne);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Fichier modifié écrit dans: " + fichierModifie);
    }
}





