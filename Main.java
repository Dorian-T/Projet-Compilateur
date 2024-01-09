import org.antlr.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");
        try {
            CharStream input = CharStreams.fromFileName("entrees.txt");
            grammarTCLLexer lexer = new grammarTCLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            System.out.println(lexer.getAllTokens());

            grammarTCLParser parser = new grammarTCLParser(tokens);
            grammarTCLParser.MainContext tree = parser.main();
            

            //System.out.println(tree);
            //Implémenter une classe NPIStack qui hérite de la classe AbstractParseTree
            //Visitor<Void> et qui implémente l’interface NPIVisitor<Void>
            
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
};


//antlr4-parse grammarTCL.g4 main -tree