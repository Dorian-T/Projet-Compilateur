import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.Type;
import Type.UnknownType;
import Type.Primitive_Type;
import Type.ArrayType;
import Type.FunctionType;

public class TyperVisitor extends AbstractParseTreeVisitor<Type> implements grammarTCLVisitor<Type> {

    private Map<UnknownType,Type> types = new HashMap<UnknownType,Type>();

    public Map<UnknownType, Type> getTypes() {
        return types;
    }

    @Override
    public Type visitNegation(grammarTCLParser.NegationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNegation'");
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitComparison'");
    }

    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOr'");
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOpposite'");
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new Primitive_Type(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_access'");
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBrackets'");
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAnd'");
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        System.out.println("visit variable : " + ctx.getChild(0).getText());
        if(this.types.containsKey(new UnknownType(ctx.getChild(0).getText(), 0))){ //on verifie si la variable existe deja dans le this.types
            return this.types.get(new UnknownType(ctx.getChild(0).getText(), 0));
        }else{
            throw new UnsupportedOperationException("Variable non déclarée");
        }
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMultiplication'");
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEquality'");
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_initialization'");
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAddition'");
    }

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {

        switch (ctx.getChild(0).getText()) {
            case "int":
                return new Primitive_Type(Type.Base.INT);
            case "bool":
                return new Primitive_Type(Type.Base.BOOL);
            case "auto":
                return new Primitive_Type(Type.Base.UNKNOWN);
            default:
                System.out.println("Erreur : type non reconnu ( //TODO : tableau )");
        }
        return null;
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        System.out.println(" --- Tableau");
        return new ArrayType(visit(ctx.getChild(0)));
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {

        System.out.println("visit declaration : " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText());
        this.types.put(new UnknownType(ctx.getChild(1)), visit(ctx.getChild(0)) );
        return null;
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPrint'");
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        System.out.println("visit assignment : " );
        //verifie si la variable existe deja dans le this.types
        if(this.types.containsKey(new UnknownType(ctx.getChild(0).getText(), 28))){ /// TODO : containsKey ne fonctionne pas comme ca
            
            //verifie si le type de la variable est le meme que celui de l'expression
            Type type1 = this.types.get(new UnknownType(ctx.getChild(0).getText(), 28));
            Type type2 = visit(ctx.getChild(2));
            System.out.println(" - type1 : " + type1.toString());
            System.out.println(" - type2 : " + type2.toString());
            if(type1.equals(type2)){ //on verifie si les types sont les memes
                System.out.println(" --- type1 == type2");
                return null;
            }else{
                //TODO : essaye de faire un unification
                throw new UnsupportedOperationException("Type non compatible");
            }
        }else{

            System.out.println(this.types.toString());
            System.out.println(new UnknownType(ctx.getChild(0).getText(), 28).toString());
            throw new UnsupportedOperationException("Variable non déclarée");
        }
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlock'");
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIf'");
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhile'");
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFor'");
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturn'");
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {

        // il faut verifier que le type de retour est le meme que celui de l'expression
        // TODO : il faut donc retourner le type de l'unification des types des return...

        // ArrayList<Type> returnType = null;
        // for(int i = 0; i < ctx.getChildCount(); i++){
        //     System.out.println("visit core_fct : " + ctx.getChild(i).getText());
        //     if(ctx.getChild(i).getText() == "return" ){
        //         i++;
        //         System.out.println(" - return");
        //         returnType.add(visit(ctx.getChild(i)));
        //     }
        // }
        // return returnType;
        this.visitChildren(ctx);
        return null;
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        
        System.out.println("visit Decl_fct : " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText());

        ArrayList<Type> args = new ArrayList<Type>();
        for(int i = 3; i < ctx.getChildCount()-1; i+=3){

            System.out.println(" - parametre : " + ctx.getChild(i).getText());

            args.add(visit(ctx.getChild(i)));
        }
        
        System.out.println("fin boucle");
        this.types.put(new UnknownType(ctx.getChild(1)), new FunctionType(visit(ctx.getChild(0)), args));
        visit(ctx.getChild(ctx.getChildCount()-1)); // on visite ensuite le bloc de la fonction
        return null;
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        System.out.println("visit main");
        types = new HashMap<UnknownType,Type>();
        this.types.put(new UnknownType(ctx), new Primitive_Type(Type.Base.INT));
        
        return this.visitChildren(ctx);
    }
}