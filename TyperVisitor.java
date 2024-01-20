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
        if(this.types.containsKey(new UnknownType(ctx))){ //on verifie si la variable existe deja dans le this.types
            return this.types.get(new UnknownType(ctx));
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
                return new UnknownType();
            default:
                throw new UnsupportedOperationException("Erreur : type non reconnu : " + ctx.getChild(0).getText());
        }
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        return new ArrayType(visit(ctx.getChild(0)));
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        System.out.println("visit declaration : " );
        if(this.types.containsKey(new UnknownType(ctx.getChild(1)))){ //on verifie si la variable existe deja dans le this.types
            throw new UnsupportedOperationException("Variable déjà déclarée");
        }

        if(visit(ctx.getChild(0)) instanceof UnknownType){ //si le type est auto, on met un UnknownType
            this.types.put(new UnknownType(ctx.getChild(1)),new UnknownType(ctx.getChild(1)));
        }else{  //sinon on met le type
            this.types.put(new UnknownType(ctx.getChild(1)), visit(ctx.getChild(0)));
        }
        
        if(ctx.getChildCount() > 3){ //declaration avec initialisation
            System.out.println(" - initialisation ");

            join(this.types.get(new UnknownType(ctx.getChild(1))).unify(visit(ctx.getChild(3))));
        }
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
        System.out.println(" - variable : " + ctx.getChild(0));
        if(this.types.containsKey(new UnknownType(ctx.getChild(0)))){
            //verifie si le type de la variable est le meme que celui de l'expression
            join(this.types.get(new UnknownType(ctx.getChild(0))).unify(visit(ctx.getChild(2))));
        }else{
            throw new UnsupportedOperationException("Variable non déclarée");
        }
        return null;
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
        System.out.println("visit return : " + ctx.getChild(1).getText());
        return visit(ctx.getChild(1));
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {

        // il faut verifier que le type de retour est le meme que celui de l'expression
        // il faut retourner le type de l'unification des types des return...

        Type returnType = visit(ctx.getChild(ctx.getChildCount() - 3));
        Map<UnknownType, Type> result = new HashMap<UnknownType, Type>();

        for(int i = 1; i < ctx.getChildCount() - 4; i++){

            result = returnType.unify(visit(ctx.getChild(i)));
            if(result != null && result.containsKey(returnType)){
                returnType = result.get(returnType);
            }
            join(result);
        }

        return returnType; //Retroune les types de retour
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        
        System.out.println("visit Decl_fct : " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText());

        ArrayList<Type> args = new ArrayList<Type>();
        for(int i = 3; i < ctx.getChildCount()-2; i+=3){

            System.out.println(" - parametre : " + ctx.getChild(i).getText());

            args.add(visit(ctx.getChild(i)));
        }
        
        this.types.put(new UnknownType(ctx.getChild(1)), new FunctionType(visit(ctx.getChild(0)), args));

        Type type_retour = visit(ctx.getChild(ctx.getChildCount()-1)); // on visite ensuite le bloc de la fonction
        Type type_retour_fonction = ((FunctionType)this.types.get(new UnknownType(ctx.getChild(1)))).getReturnType();
        join(type_retour_fonction.unify(type_retour));

        return null; //on ne retourne rien
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        System.out.println("========{visit main}========");
        types = new HashMap<UnknownType,Type>();

        this.types.put(new UnknownType(ctx.getChild(ctx.getChildCount()-3)), new Primitive_Type(Type.Base.INT));
        
        
        
        Type typeretour = this.visitChildren(ctx); // on visite les fils et on recupere le type de retour

        if(typeretour != null){
            System.out.println(" - type de retour : " + typeretour);
            if(typeretour.equals(new Primitive_Type(Type.Base.INT))){
                throw new UnsupportedOperationException("Le type de retour de la fonction main n'est pas entier");
            }
        }
        System.out.println("========{fin visit main}========");

        return null;
    }

    public void join(Map<UnknownType,Type> h){
        if(h == null){
            return;
        }
        for (UnknownType key : h.keySet()){
            this.types.remove(key);
            this.types.put(key, h.get(key));
        }
    }











    // public Type Join(Type t1, Type t2){
    //     if(t1.equals(t2)){
    //         return t1;
    //     }
    //     else if(t1 instanceof UnknownType && t2 instanceof UnknownType){
    //         /// TODO : retenir que les deux types sont les memes... je sais pas comment...
    //         return t1;
    //     }
    //     else if(t1 instanceof UnknownType){
    //         this.types.remove((UnknownType)t1);
    //         this.types.put((UnknownType)t1, t2);
    //         return t2;
    //     }
    //     else if(t2 instanceof UnknownType){
    //         this.types.remove((UnknownType)t2);
    //         this.types.put((UnknownType)t2, t1);
    //         return t1;
    //     }
    //     else if(t1 instanceof ArrayType && t2 instanceof ArrayType){
    //         return new ArrayType(Join(((ArrayType)t1),((ArrayType)t2)));
    //     }
    //     else{
    //         throw new UnsupportedOperationException("Les types ne sont pas unifiables");
    //     }
    // }

    // public Type Join(ArrayType t1, ArrayType t2){
    //     if(t1.getTabType().equals(t2.getTabType())){
    //         return t1;
    //     }
    //     else if(t1.getTabType() instanceof UnknownType && t2.getTabType() instanceof UnknownType){
    //         /// TODO : retenir que les deux types sont les memes... je sais pas comment...
    //         return t1;
    //     }
    //     else if(t1.getTabType() instanceof UnknownType){
    //         this.types.remove((UnknownType)t1.getTabType());
    //         this.types.put((UnknownType)t1.getTabType(), t2);
    //         return t2;
    //     }
    //     else if(t2.getTabType() instanceof UnknownType){
    //         this.types.remove((UnknownType)t2.getTabType());
    //         this.types.put((UnknownType)t2.getTabType(), t1);
    //         return t1;
    //     }
    //     else if(t1.getTabType() instanceof ArrayType && t2.getTabType() instanceof ArrayType){
    //         return new ArrayType(Join(((ArrayType)t1.getTabType()),((ArrayType)t2.getTabType())));
    //     }
    //     else{
    //         throw new UnsupportedOperationException("Les types ne sont pas unifiables");
    //     }
    // }
}