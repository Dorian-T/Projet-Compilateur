import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import Asm.Program;


public class Main {
    public static void main(String[] args) {
        //cahrgement du fichier
        CharStream input = null;
        try {
            input = CharStreams.fromFileName("entrees.txt");
        }catch (Exception e){
            System.out.println("type d'erreure : " + e.getClass());
            System.out.println(e.getMessage());
        }

        //rappel du fichier + initialisation du lexer et du parser
        System.out.println("input :  \n " + input.toString());
        grammarTCLLexer lexer = new grammarTCLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        grammarTCLParser parser = new grammarTCLParser(tokens);
        grammarTCLParser.MainContext tree = parser.main();
        
        //-----------------GROUPE 1 : TyperVisitor-----------------
        
        TyperVisitor typer = new TyperVisitor();
        typer.visit(tree);

        //-----------------GROUPE 2 : CodeGenerator-----------------
        
        CodeGenerator codeGenerator = new CodeGenerator(typer.getTypes());
        Program program =codeGenerator.visit(tree);
        toFile(program);
    // on met dans un fichier


    }

    public static void toFile(Program program){
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


//antlr4-parse grammarTCL.g4 main -tree