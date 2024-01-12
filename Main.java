import org.antlr.runtime.tree.ParseTree;
import org.antlr.v4.gui.SystemFontMetrics;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import Asm.Instruction;
import Asm.Program;
import Asm.UAL;

public class Main {
    public static void main(String[] args) {

        try {
            CharStream input = CharStreams.fromFileName("entrees.txt");
			System.out.println("input :  \n " + input.toString());
            grammarTCLLexer lexer = new grammarTCLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            grammarTCLParser parser = new grammarTCLParser(tokens);
            grammarTCLParser.MainContext tree = parser.main();
            
            //-----------------GROUPE 1 : TyperVisitor-----------------
			TyperVisitor typer = new TyperVisitor();
            typer.visit(tree);
            typer.getTypes();
            //Map<UnknownType, Type> types

            //-----------------GROUPE 2 : CodeGenerator-----------------
            
            System.out.println("Codegen ; \n");
			CodeGenerator codeGenerator = new CodeGenerator(null);
			Program program =codeGenerator.visit(tree);
			System.out.println(program);

            
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }
};


//antlr4-parse grammarTCL.g4 main -tree