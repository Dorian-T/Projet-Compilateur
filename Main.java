import Graph.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import Asm.Program;
import Graph.GrapheDeConflits;
import Graph.GrapheDeControle;
import Graph.UnorientedGraph;
import Graph.Variables;

import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // cahrgement du fichier
        CharStream input = null;
        try {
            input = CharStreams.fromFileName("entrees.txt");
        } catch (Exception e) {
            System.out.println("type d'erreure : " + e.getClass());
            System.out.println(e.getMessage());
        }

        // rappel du fichier + initialisation du lexer et du parser
        System.out.println("input :  \n " + input.toString() + "\n");
        grammarTCLLexer lexer = new grammarTCLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        grammarTCLParser parser = new grammarTCLParser(tokens);
        grammarTCLParser.MainContext tree = parser.main();

        // -----------------GROUPE 1 : TyperVisitor-----------------

        TyperVisitor typer = new TyperVisitor();
        typer.debug = false; // passer à true pour afficher les modifications de la map pas à pas
        typer.visit(tree);

        // -----------------GROUPE 2 : CodeGenerator-----------------

        CodeGenerator codeGenerator = new CodeGenerator(typer.getTypes());
        Program program = codeGenerator.visit(tree);
        toFile(program);
        // on met dans un fichier

        // -------------------GROUPE 3 : Graph-----------------------
        // QUESTION1
        String file = "prog.asm";
        List<String> instructions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String ligne;
            String labelEnCours = ""; // Utilisé pour stocker un label jusqu'à la prochaine instruction

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();

                // Ignorer les commentaires
                if (ligne.startsWith("#")) {
                    continue;
                }

                // Vérifier si la ligne est un label
                if (ligne.endsWith(":")) {
                    labelEnCours = ligne; // Stocker le label
                    continue;
                }

                // Concaténer le label (s'il existe) avec l'instruction actuelle
                if (!labelEnCours.isEmpty()) {
                    ligne = labelEnCours + " " + ligne;
                    labelEnCours = ""; // Réinitialiser le label en cours
                }

                instructions.add(ligne);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(instructions);
        GrapheDeControle graphe = new GrapheDeControle();
        graphe.ajouterSommets(instructions);
        graphe.ajouteAretes(instructions);

        System.out.println();
        for (int i = 0; i < instructions.size(); i++) {
            List<Integer> outNeighbors = graphe.getOutNeighbors(i);
            List<Integer> inNeighbors = graphe.getInNeighbors(i);

            System.out.println("Instruction " + i + " : " + instructions.get(i));
            System.out.println("Voisins sortants: " + convertIndicesToInstructions(outNeighbors, instructions));
            System.out.println("Voisins entrants: " + convertIndicesToInstructions(inNeighbors, instructions));
            System.out.println();
        }

        // QUESTION 2
        System.out.println("\n-----------------------------------------\n");

        // QUESTION 3
        // Analyse des variables vivantes
        Variables variables = new Variables(graphe, instructions);
        variables.calculateLVEntryExit();

        // Construire un HashMap pour LVExit pour chaque instruction
        // Construire un HashMap pour LVExit pour chaque instruction
        HashMap<Integer, Set<Integer>> lvExitMap = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Set<Integer> lvExitSet = variables.getLVExit(i);
            lvExitMap.put(i, lvExitSet);
        }

        // Afficher les résultats de l'analyse des variables vivantes
        for (int i = 0; i < instructions.size(); i++) {
            String instr = instructions.get(i);
            System.out.println("Instruction: " + instr);
            System.out.println("Variables générées: " + variables.getGeneratedVariables(i));
            System.out.println("Variables tuées: " + variables.getKilledVariables(i));
            System.out.println("LVEntry: " + variables.getLVEntry(i));
            System.out.println("LVExit: " + variables.getLVExit(i));
            System.out.println();
        }

        System.out.println("\n-----------------------------------------\n");

        // Construire le Graphe de Conflit
        HashMap<String, Set<Integer>> lvExitMapForConflits = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            lvExitMapForConflits.put(instructions.get(i), lvExitMap.get(i));
        }
        Set<Integer> tousLesRegistres = new HashSet<>();
        for (String instr : instructions) {
            // Extrait les registres de chaque instruction et les ajoute à l'ensemble
            String[] parts = instr.split("\\s+");
            for (String part : parts) {
                if (part.matches("R\\d+")) {
                    tousLesRegistres.add(Integer.parseInt(part.substring(1)));
                }
            }
        }

        // Construire le Graphe de Conflit avec tous les registres
        GrapheDeConflits grapheDeConflit = new GrapheDeConflits(lvExitMapForConflits, tousLesRegistres);

        UnorientedGraph<Integer> grapheConflit = grapheDeConflit.getGrapheConflit();

        // Afficher le graphe de conflit
        System.out.println("Graphe de Conflit:\n" + grapheConflit);

        // Appliquer la coloration du graphe de conflit
        int nombreDeCouleurs = grapheDeConflit.colorerGraphe();
        System.out.println("Nombre de couleurs utilisées pour la coloration: " + nombreDeCouleurs);
        // Obtenir la coloration pour chaque registre

        HashMap<Integer, Integer> colorationRegistres = new HashMap<>();
        for (int reg : grapheConflit.vertices) {
            int color = grapheConflit.getColor(reg);
            colorationRegistres.put(reg, color); // Associer chaque registre à sa couleur
            System.out.println("Registre: R" + reg + ", Couleur: " + color);
        }

        String fichierModifie = "sortie.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(file));
                BufferedWriter bw = new BufferedWriter(new FileWriter(fichierModifie))) {

            String ligne;
            while ((ligne = br.readLine()) != null) {
                for (Map.Entry<Integer, Integer> entry : colorationRegistres.entrySet()) {
                    // Utiliser une expression régulière pour matcher les registres en tant que mots
                    // complets
                    ligne = ligne.replaceAll("\\bR" + entry.getKey() + "\\b", "R" + entry.getValue());
                }
                bw.write(ligne);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Fichier modifié écrit dans: " + fichierModifie);

    }

    private static List<String> convertIndicesToInstructions(List<Integer> indices, List<String> instructions) {
        List<String> neighborInstructions = new ArrayList<>();
        if (indices != null) {
            for (Integer index : indices) {
                neighborInstructions.add(instructions.get(index));
            }
        }
        return neighborInstructions;
    }

    public static void toFile(Program program) {
        try {
            FileWriter fileWriter = new FileWriter("prog.asm", false);

            BufferedWriter writer = new BufferedWriter(fileWriter);

            writer.write(program.toString());

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
};

// antlr4-parse grammarTCL.g4 main -tree