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
        System.out.println("input :  \n " + input.toString());
        grammarTCLLexer lexer = new grammarTCLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        grammarTCLParser parser = new grammarTCLParser(tokens);
        grammarTCLParser.MainContext tree = parser.main();

        // -----------------GROUPE 1 : TyperVisitor-----------------

        TyperVisitor typer = new TyperVisitor();
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
        for (int i = 0; i < nombreDeCouleurs; i++) { // Supposons que les registres sont numérotés de 0 à
                                                     // nombreDeCouleurs-1
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

    public static void toFile(Program program) {
        try {
            // Création d'un fileWriter pour écrire dans un fichier
            FileWriter fileWriter = new FileWriter("prog.asm", false);

            // Création d'un bufferedWriter qui utilise le fileWriter
            BufferedWriter writer = new BufferedWriter(fileWriter);

            // ajout d'un texte à notre fichier
            writer.write(program.toString());

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
};

// antlr4-parse grammarTCL.g4 main -tree