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
        visitChildren(ctx);
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        
        Type var1 = visit(ctx.getChild(0));
        if(var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType)var1).unify(new Primitive_Type(Type.Base.INT)),var1);

        Type var2 = visit(ctx.getChild(2));
        if(var2 instanceof UnknownType)
            var2 = addInTypesMap(((UnknownType)var2).unify(new Primitive_Type(Type.Base.INT)),var2);

        //les deux types doivent etre des entiers(les UnknownType ont été remplacés par des entiers)
        if(!(var1.equals(new Primitive_Type(Type.Base.INT)) && var2.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException("Les types comparés ( " + ctx.getChild(1).getText() + " ) ne sont pas des entiers");
        
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        visitChildren(ctx);
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Type var1 = visit(ctx.getChild(1));
        if(var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType)var1).unify(new Primitive_Type(Type.Base.INT)),var1);

        //le type doit etre un entier(les UnknownType ont été remplacés par des entiers)
        if(!(var1.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException("type opposé ( " + ctx.getChild(1).getText() + " ) pas un entier");

        return null;
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new Primitive_Type(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        // TODO:tableau
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_access'");
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        return visit(ctx.getChild(1));
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        System.out.println(" - visit call : " + ctx.getChild(0).getText());

        // TODO:fonction
        throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        visitChildren(ctx);
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        System.out.println("   - visit variable : " + ctx.getChild(0).getText());

        //on verifie si la variable existe deja dans le this.types
        if(this.types.containsKey(new UnknownType(ctx)))
            return this.types.get(new UnknownType(ctx));
        else
            throw new UnsupportedOperationException("Variable non déclarée");

    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Type var1 = visit(ctx.getChild(0));
        if(var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType)var1).unify(new Primitive_Type(Type.Base.INT)),var1);
        
        Type var2 = visit(ctx.getChild(2));
        if(var2 instanceof UnknownType)
            var2 = addInTypesMap(((UnknownType)var2).unify(new Primitive_Type(Type.Base.INT)),var2);

        //les deux types doivent etre des entiers(les UnknownType ont été remplacés par des entiers)
        if(!(var1.equals(new Primitive_Type(Type.Base.BOOL)) && var2.equals(new Primitive_Type(Type.Base.BOOL))))
            throw new UnsupportedOperationException("Les types multipliés ( " + ctx.getChild(1).getText() + " ) ne sont pas des entiers");
    
        return new Primitive_Type(Type.Base.INT);
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
       
        addInTypesMap(visit(ctx.getChild(0)).unify(visit(ctx.getChild(2))));
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_initialization'");
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        Type var1 = visit(ctx.getChild(0));
        if(var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType)var1).unify(new Primitive_Type(Type.Base.INT)),var1);
        
        Type var2 = visit(ctx.getChild(2));
        if(var2 instanceof UnknownType)
            var2 = addInTypesMap(((UnknownType)var2).unify(new Primitive_Type(Type.Base.INT)),var2);

        //les deux types doivent etre des entiers(les UnknownType ont été remplacés par des entiers)
        if(!(var1.equals(new Primitive_Type(Type.Base.INT)) && var2.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException("Les types Addition ( " + ctx.getChild(1).getText() + " ) ne sont pas des entiers");
    
        return new Primitive_Type(Type.Base.INT);
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
        System.out.println(" - visit declaration " );
        
        //on verifie si la variable existe deja dans le this.types
        if(this.types.containsKey(new UnknownType(ctx.getChild(1)))) 
            throw new UnsupportedOperationException("Variable déjà déclarée");
        
        
        if(!containsVar(visit(ctx.getChild(0))))
            //si le type n'est pas auto, et ne contient aucun auto (tab de auto...)
            this.types.put(new UnknownType(ctx.getChild(1)), visit(ctx.getChild(0)));
        else
            //sinon on prend le type et on remplace les auto par des #[nom de la variable].
            this.types.put(new UnknownType(ctx.getChild(1)), visit(ctx.getChild(0)).substitute(new UnknownType(), new UnknownType("#" + ctx.getChild(1).getText())));
        
        //declaration avec initialisation ?
        if(ctx.getChildCount() > 3){ 
            System.out.println("   + initialisation ");
            addInTypesMap(this.types.get(new UnknownType(ctx.getChild(1))).unify(visit(ctx.getChild(3))));
        }

        //aucun retrun possible.
        return null;
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        System.out.println(" - visit assignment " );

        //on unifie et on ajoute dans this.types si necessaire grace a addInTypesMap !
        addInTypesMap(this.types.get(new UnknownType(ctx.getChild(0).getText())).unify(visit(ctx.getChild(2))));
        return null;
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        System.out.println(" - visit block : " );
        Type typeretour = visit(ctx.getChild(1));
        for(int i = 2; i < ctx.getChildCount() - 1; i++){
            System.out.println("   + visit block : " + i );
            if(typeretour != null)
                typeretour = addInTypesMap(typeretour.unify(typeretour), visit(ctx.getChild(i)));
            else
                typeretour = visit(ctx.getChild(i));
        }
        System.out.println(" + fin visit block : " + typeretour);
        return typeretour;
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        System.out.println("visit if" );

        // Verification que les deux éléments du test sont du meme type
        if (!visit(ctx.getChild(2).getChild(0)).equals(visit(ctx.getChild(2).getChild(2)))) {
            throw new UnsupportedOperationException("Les deux éléments du test ne sont pas du meme type");
        }

        Type blocIf = visit(ctx.getChild(4));
        Type blocElse = null;
        if(ctx.getChildCount() > 5) // il y a un else
            blocElse = visit(ctx.getChild(6));

        if(blocIf == null && blocElse == null)
            return null;
        else if(blocIf != null && blocElse == null)
            return blocIf;
        else if(blocIf == null)
            return blocElse;
        else
            return addInTypesMap(blocIf.unify(blocElse), blocIf);
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        System.out.println(" - visit while" );

        //on visite la condition
        visit(ctx.getChild(2));

        //on visite le bloc et on retourne le type de retour
        return visit(ctx.getChild(4));
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        visit(ctx.getChild(2));
        visit(ctx.getChild(4));
        visit(ctx.getChild(6));
        return visit(ctx.getChild(8));
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        System.out.println("visit return : " + ctx.getChild(1).getText());
        return visit(ctx.getChild(1));
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        System.out.println("visit core_fct : " );
        // il faut verifier que le type de retour est le meme que celui de l'expression
        // il faut retourner le type de l'unification des types des return...

        Type returnType = visit(ctx.getChild(ctx.getChildCount() - 3));

        for(int i = 1; i < ctx.getChildCount() - 4; i++){

            returnType = addInTypesMap(returnType.unify(visit(ctx.getChild(i))), returnType);
        }
        return returnType; //Retroune les types de retour
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        
        //TODO:fonction
        System.out.println("visit Decl_fct : " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText());

        ArrayList<Type> args = new ArrayList<Type>();
        for(int i = 3; i < ctx.getChildCount()-2; i+=3){

            System.out.println(" - parametre : " + ctx.getChild(i).getText());

            args.add(visit(ctx.getChild(i)));
        }
        
        this.types.put(new UnknownType(ctx.getChild(1)), new FunctionType(visit(ctx.getChild(0)), args));

        Type type_retour = visit(ctx.getChild(ctx.getChildCount()-1)); // on visite ensuite le bloc de la fonction
        Type type_retour_fonction = ((FunctionType)this.types.get(new UnknownType(ctx.getChild(1)))).getReturnType();
        type_retour_fonction.unify(type_retour); 

        return null; //on ne retourne rien
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        System.out.println("========{visit main}========");
        
        types = new HashMap<UnknownType,Type>();

        this.types.put(new UnknownType(ctx.getChild(ctx.getChildCount()-3)), new Primitive_Type(Type.Base.INT));
        
        // on visite les fils (les fonctions)
        for(int i = 0; i < ctx.getChildCount() - 3; i++){
            visit(ctx.getChild(i));
        }

        // on visite le bloc de la fonction main
        Type typeretour = this.visit(ctx.getChild(ctx.getChildCount()-2)); 

        if(typeretour == null)
            throw new UnsupportedOperationException("La fonction main ne retourne rien");

        if(!typeretour.equals(new Primitive_Type(Type.Base.INT))){
            throw new UnsupportedOperationException("Le type de retour de la fonction main n'est pas entier");
        }

        System.out.println("========{fin visit main}========");

        return null;
    }

    //appelle clasique de cette fonction : returnType = this.addInTypesMap(returnType.unify(visit(ctx.getChild(i))), returnType);
    public Type addInTypesMap(Map<UnknownType,Type> modifMap, Type returnType){
        
        if (modifMap == null) // s il n'y a pas eu de changement
            return returnType;

        //System.out.println(" - addInTypesMap : " + modifMap );
        
        this.types.putAll(modifMap);
        
        //TODO : verifier qu'il n'y a pas une boucle infinie
        //on verifie si returnType contien une variable et si il y a eu des changements
        while ( containsVar(returnType) && !(returnType.equals(returnType.substituteAll(this.types))) ){ 
            
            //on fait les substitutions
            returnType = returnType.substituteAll(this.types);
        }

        //on fait les substitutions dans this.types
        for(UnknownType key : this.types.keySet()){
            if(containsVar(this.types.get(key)) && !(this.types.get(key).equals( this.types.get(key).substituteAll(this.types) )) ){
                this.types.put(key, this.types.get(key).substituteAll(this.types));
            }
        }
        //System.out.println(" - returnType de addInTypesMap : " + returnType);
        return returnType;
        // TODO : suprime si plus utilisé les var de fin de tableau (ex : #a , #variable ... ) on le fera probablement a la fin de la visite de la fonction main
    }

    public void addInTypesMap(Map<UnknownType,Type> modifMap){
        addInTypesMap(modifMap, null);
    }

    public boolean containsVar(Type t){
        if(t instanceof UnknownType)
            return true;
        if(t instanceof ArrayType)
            return containsVar(((ArrayType)t).getTabType());
        return false;
    }

    // public void join(Map<UnknownType,Type> h){
    //     if(h == null){
    //         return;
    //     }
    //     for (UnknownType key : h.keySet()){
    //         this.types.remove(key);
    //         this.types.put(key, h.get(key));
    //     }
    // }

    // public Type Join(Type t1, Type t2){
    //     if(t1.equals(t2)){
    //         return t1;
    //     }
    //     else if(t1 instanceof UnknownType && t2 instanceof UnknownType){
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