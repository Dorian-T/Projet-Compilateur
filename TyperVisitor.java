import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.Type;
import Type.UnknownType;
import Type.Primitive_Type;
import Type.ArrayType;
import Type.FunctionType;

public class TyperVisitor extends AbstractParseTreeVisitor<Type> implements grammarTCLVisitor<Type> {

    private Map<UnknownType, Type> types = new HashMap<UnknownType, Type>();
    public boolean debug = false;

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
        if (var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType) var1).unify(new Primitive_Type(Type.Base.INT)), var1);

        Type var2 = visit(ctx.getChild(2));
        if (var2 instanceof UnknownType)
            var2 = addInTypesMap(((UnknownType) var2).unify(new Primitive_Type(Type.Base.INT)), var2);

        // les deux types doivent etre des entiers(les UnknownType ont été remplacés par
        // des entiers)
        if (!(var1.equals(new Primitive_Type(Type.Base.INT)) && var2.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException(
                    "Les types comparés ( " + ctx.getChild(1).getText() + " ) ne sont pas des entiers");

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
        if (var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType) var1).unify(new Primitive_Type(Type.Base.INT)), var1);

        // le type doit etre un entier(les UnknownType ont été remplacés par des
        // entiers)
        if (!(var1.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException("type opposé ( " + ctx.getChild(1).getText() + " ) pas un entier");

        return null;
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new Primitive_Type(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {

        // on verifie que l'index est un entier
        Type index = visit(ctx.getChild(2));
        if (index instanceof UnknownType)
            index = addInTypesMap(((UnknownType) index).unify(new Primitive_Type(Type.Base.INT)), index);

        if (!(index.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException("L'index du tableau n'est pas un entier");

        // on verifie que la variable est un tableau
        Type var = visit(ctx.getChild(0));
        if (var instanceof ArrayType)
            return ((ArrayType) var).getTabType();

        if (var instanceof UnknownType) {
            addInTypesMap((UnknownType) var, new ArrayType(var));
            return var;
        }

        throw new UnsupportedOperationException("La variable n'est pas un tableau");
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        return visit(ctx.getChild(1));
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {

        // on verifie que la fonction existe
        if (!this.types.containsKey(new UnknownType(ctx.getChild(0))))
            throw new UnsupportedOperationException("call sur fonction non déclarée");

        FunctionType type_fonction = (FunctionType) this.types.get(new UnknownType(ctx.getChild(0)));

        // on recupere les arguments de l appel de fonction
        ArrayList<Type> args_call = new ArrayList<Type>();
        for (int i = 2; i < ctx.getChildCount() - 1; i += 2) {
            args_call.add(visit(ctx.getChild(i)));
        }

        // on verifie que le nombre d'arguments est le meme que celui de la fonction
        if (args_call.size() != type_fonction.getNbArgs())
            throw new UnsupportedOperationException("Nombre d'arguments incorrect");

        // on rassemble les arguments de l'appel avec ceux de la fonction (on fait juste une meme map pour les metre face a face)
        Map<Type, Type> fusion_after_call = new HashMap<>();
        for (int i = 0; i < args_call.size(); i++) {

            if (containsVar(type_fonction.getArgsType(i)))
                // si le type de l'argument contient une variable, on le met dans la map ... pour le traiter plus tard...
                fusion_after_call.put(args_call.get(i), type_fonction.getArgsType(i));
            else
                // si le type de l'argument ne contient pas de variable, on le traite directement
                addInTypesMap(args_call.get(i).unify(type_fonction.getArgsType(i)));
        }
        
        // on simplifie un maximum notre map
        Map<Type,UnknownType> fusion_after_call_term = fusion_after_call.entrySet().stream()
                .map(entry -> {
                    // on simplifie si il y a des ArrayType des deux cotés
                    while (entry.getKey() instanceof ArrayType && entry.getValue() instanceof ArrayType) {
                        ArrayType key = (ArrayType) entry.getKey();
                        ArrayType value = (ArrayType) entry.getValue();

                        entry = new HashMap.SimpleEntry<Type, Type>(key.getTabType(), value.getTabType());
                    }
                    
                    // on gere le cas ou il y a un ArrayType a droite
                    if(entry.getValue() instanceof ArrayType ){
                        if(!(entry.getKey() instanceof UnknownType))
                            throw new UnsupportedOperationException("Les types des arguments ne correspondent pas (tableau et Primitive_Type)");
                        
                        Type key = entry.getKey();
                        Type value = entry.getValue();
                        while(value instanceof ArrayType){
                            key = new ArrayType(key);
                            value = ((ArrayType)value).getTabType();
                        }
                        addInTypesMap((UnknownType)entry.getKey(), key);
                        entry = new HashMap.SimpleEntry<Type, Type>(entry.getKey(),value);
                    }
                    // maintenant il ne reste plus que des UnknownType a droite
                    
                    return new HashMap.SimpleEntry<Type, UnknownType> (entry.getKey(), (UnknownType)entry.getValue());
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // on echanger les key et value dans fusion_after_call_ret
        Map<UnknownType, Type> fusion_after_call_ret = fusion_after_call_term.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        
        // on fait les substitutions et on envoye les resultats dans this.types
        for(Entry<Type,UnknownType> entry : fusion_after_call_term.entrySet()){
            addInTypesMap(entry.getKey().unify(entry.getValue().substituteAll(fusion_after_call_ret)));        
        }

        return type_fonction.getReturnType().substituteAll(fusion_after_call_ret);
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

        // on verifie si la variable existe deja dans le this.types
        if (this.types.containsKey(new UnknownType(ctx)))
            return this.types.get(new UnknownType(ctx));
        else
            throw new UnsupportedOperationException("Variable non déclarée");

    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Type var1 = visit(ctx.getChild(0));
        if (var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType) var1).unify(new Primitive_Type(Type.Base.INT)), var1);

        Type var2 = visit(ctx.getChild(2));
        if (var2 instanceof UnknownType)
            var2 = addInTypesMap(((UnknownType) var2).unify(new Primitive_Type(Type.Base.INT)), var2);

        // les deux types doivent etre des entiers(les UnknownType ont été remplacés par
        // des entiers)
        if (!(var1.equals(new Primitive_Type(Type.Base.INT)) && var2.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException(
                    "Les types multipliés ( " + ctx.getChild(1).getText() + " ) ne sont pas des entiers");

        return new Primitive_Type(Type.Base.INT);
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {

        addInTypesMap(visit(ctx.getChild(0)).unify(visit(ctx.getChild(2))));
        return new Primitive_Type(Type.Base.BOOL);
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {

        Type typeretour = null;
        // on parcours les elements du tableau et on verifie qu'ils sont du meme type
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            if (typeretour == null)
                typeretour = visit(ctx.getChild(i));
            else
                typeretour = addInTypesMap(typeretour.unify(visit(ctx.getChild(i))), typeretour);
        }
        return new ArrayType(typeretour);
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        Type var1 = visit(ctx.getChild(0));
        if (var1 instanceof UnknownType)
            var1 = addInTypesMap(((UnknownType) var1).unify(new Primitive_Type(Type.Base.INT)), var1);

        Type var2 = visit(ctx.getChild(2));
        if (var2 instanceof UnknownType)
            var2 = addInTypesMap(((UnknownType) var2).unify(new Primitive_Type(Type.Base.INT)), var2);

        // les deux types doivent etre des entiers(les UnknownType ont été remplacés par
        // des entiers)
        if (!(var1.equals(new Primitive_Type(Type.Base.INT)) && var2.equals(new Primitive_Type(Type.Base.INT))))
            throw new UnsupportedOperationException(
                    "Les types Addition ( " + ctx.getChild(1).getText() + " ) ne sont pas des entiers");

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

        // on verifie si la variable existe deja dans le this.types
        if (this.types.containsKey(new UnknownType(ctx.getChild(1))))
            throw new UnsupportedOperationException("Variable déjà déclarée (nom deja utilisé)");

        if (!containsVar(visit(ctx.getChild(0))))
            // si le type n'est pas auto, et ne contient aucun auto (tab de auto...)
            addInTypesMap(new UnknownType(ctx.getChild(1)), visit(ctx.getChild(0)));
        else
            // sinon on prend le type et on remplace les auto par des #[nom de la variable].
            addInTypesMap(new UnknownType(ctx.getChild(1)), visit(ctx.getChild(0)).substitute(new UnknownType(),
                    new UnknownType("#" + ctx.getChild(1).getText())));
            

        // declaration avec initialisation ?
        if (ctx.getChildCount() > 3) {
            addInTypesMap(this.types.get(new UnknownType(ctx.getChild(1))).unify(visit(ctx.getChild(3))));
        }

        // aucun retrun possible.
        return null;
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {

        Type var = new UnknownType(ctx.getChild(0));
        Type value = visit(ctx.getChild(ctx.getChildCount() - 2));

        for (int i = 2; i < ctx.getChildCount() - 4; i += 3) {
            value = new ArrayType(value);
        }
        // on unifie et on ajoute dans this.types si necessaire grace a addInTypesMap !
        addInTypesMap(this.types.get(var).unify(value));
        return null;
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {

        // on visite le premier fils pour initialiser typeretour.
        Type typeretour = null;

        // on visite les autres fils et on unifie les types de retour avec addInTypesMap
        // !
        for (int i = 1; i < ctx.getChildCount() - 1; i++) {
            if (typeretour != null)
                typeretour = addInTypesMap(typeretour.unify(visit(ctx.getChild(i))), typeretour);
            else
                typeretour = visit(ctx.getChild(i));
        }
        return typeretour;
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {

        // condition
        visit(ctx.getChild(2));

        // bloc
        Type blocIf = visit(ctx.getChild(4));
        Type blocElse = null;
        if (ctx.getChildCount() > 5) // il y a un else
            blocElse = visit(ctx.getChild(6));

        if (blocIf == null && blocElse == null)
            return null;
        else if (blocIf != null && blocElse == null)
            return blocIf;
        else if (blocIf == null)
            return blocElse;
        else
            return addInTypesMap(blocIf.unify(blocElse), blocIf);
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {

        // on visite la condition
        visit(ctx.getChild(2));

        // on visite le bloc et on retourne le type de retour
        return visit(ctx.getChild(4));
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {

        // on visite la declaration
        visit(ctx.getChild(2));
        // on visite la condition
        visit(ctx.getChild(4));
        // on visite l'incrementation
        visit(ctx.getChild(6));

        // on visite le bloc et on retourne le type de retour possiblement null.
        return visit(ctx.getChild(8));
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        return visit(ctx.getChild(1));
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // il faut verifier que le type de retour est le meme que celui de l'expression
        // il faut retourner le type de l'unification des types des return...

        Type returnType = null;

        for (int i = 1; i < ctx.getChildCount() - 4; i++) {
            if (returnType == null)
                returnType = visit(ctx.getChild(i));
            else
                returnType = addInTypesMap(returnType.unify(visit(ctx.getChild(i))), returnType);
        }
        if (returnType == null)
            returnType = visit(ctx.getChild(ctx.getChildCount() - 3));
        else
            returnType = addInTypesMap(returnType.unify(visit(ctx.getChild(ctx.getChildCount() - 3))), returnType);
        return returnType; // Retroune les types de retour
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {

        // on gere les types retours grace a ce qui remonte de core_fct et on les unifie
        // avec le type de retour de la fonction
        Type type_retour = visit(ctx.getChild(0));

        // on gere les types des parametres en sauvgardant leur nom dans une liste pour
        // les remplacer par leurs type a la fin de visitDecl_fct.
        ArrayList<Type> nom_args = new ArrayList<Type>();

        for (int i = 3; i < ctx.getChildCount() - 2; i += 3) {

            nom_args.add(new UnknownType(ctx.getChild(i + 1)));

            if (this.types.containsKey(new UnknownType(ctx.getChild(i + 1))))
                throw new UnsupportedOperationException(
                        "Variable déjà déclarée (parametres de fonction) (nom deja utilisé)");

            if (!containsVar(visit(ctx.getChild(i))))
                // si le type n'est pas auto, et ne contient aucun auto (tab de auto...)
                addInTypesMap(new UnknownType(ctx.getChild(i + 1)), visit(ctx.getChild(i)));
            else
                // sinon on prend le type et on remplace les auto par des #[nom de la variable].
                addInTypesMap(new UnknownType(ctx.getChild(i + 1)),
                        visit(ctx.getChild(i)).substitute(new UnknownType(),
                                new UnknownType("#" + ctx.getChild(i + 1).getText())));
        }

        Type type_retour_cor_fct = visit(ctx.getChild(ctx.getChildCount() - 1));
        if (type_retour.unify(type_retour_cor_fct) != null)
            type_retour = type_retour.substituteAll(type_retour.unify(type_retour_cor_fct)); // on visite ensuite le bloc de
                                                                                            // la fonction
        ArrayList<Type> types_args = new ArrayList<Type>();
        for (int i = 0; i < nom_args.size(); i++) {
            types_args.add(this.types.get(nom_args.get(i)));
        }

        if (this.types.containsKey(new UnknownType(ctx.getChild(1)))) {
            throw new UnsupportedOperationException("Fonction déjà déclarée (nom deja utilisé)");
        }

        addInTypesMap(new UnknownType(ctx.getChild(1)), new FunctionType(type_retour, types_args));
        return null; // on ne retourne rien
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        if(debug)
            System.out.println("=========={visit}==========");

        types = new HashMap<UnknownType, Type>();

        addInTypesMap(new UnknownType(ctx.getChild(ctx.getChildCount() - 3)), new Primitive_Type(Type.Base.INT));
        
        // on visite les fils (les fonctions)
        for (int i = 0; i < ctx.getChildCount() - 3; i++) {
            visit(ctx.getChild(i));
        }

        // on visite le bloc de la fonction main
        Type typeretour = this.visit(ctx.getChild(ctx.getChildCount() - 2));

        if (typeretour == null)
            throw new UnsupportedOperationException("La fonction main ne retourne rien");

        if (!typeretour.equals(new Primitive_Type(Type.Base.INT))) {
            throw new UnsupportedOperationException("Le type de retour de la fonction main n'est pas entier");
        }
        if(debug)
            System.out.println("========{fin visit}========");

        if(debug)
            System.out.println("\nMAP FINALE : " + this.types + "\n"); // on affiche la map finale
        return null;
    }

    // appelle clasique de cette fonction :
    // returnType = this.addInTypesMap(returnType.unify(visit(ctx.getChild(i))),
    // returnType);
    // => si mopdifMap est null, on ne fait rien... (pas de changement)
    // => si returnType n'est pas null, on le met a jour avec modifMap puis
    // this.types
    // => on ajoute modifMap a this.types (souvent en provenance d'un unify entre
    // deux type de variable)
    // => on met a jour tout les types de this.types avec les changements
    // => on verifie qu'il n'y a pas de boucle infinie
    // => on supprime les variables de fin de tableau grace a une lambda expression
    public Type addInTypesMap(Map<UnknownType, Type> modifMap, Type returnType) {

        if (modifMap == null) // s il n'y a pas eu de changement
            return returnType;

        // on fait les substitutions dans returnType pour le mettre a jour et le return
        // a la fin
        if (returnType != null) {
            returnType = returnType.substituteAll(modifMap);
            returnType = returnType.substituteAll(this.types);
        }

        // on ajoute les changements dans this.types
        this.types.putAll(modifMap);

        // on fait les substitutions dans this.types pour metre a jour tout les types
        // concernés
        for (UnknownType key : this.types.keySet()) {

            this.types.put(key, this.types.get(key).substituteAll(this.types));

        }

        // on verifie qu'il n'y a pas de boucle infinie
        verifBoucleInfini();

        // on supprime les variables de fin de tableau grace a une lambda expression !!!
        // (on ne peut pas le faire dans le for each)
        this.types.keySet().removeIf(key -> key.getVarName().startsWith("#"));

        if(debug)
                System.out.println("modifMap : " + modifMap + "\nMap de types : " + this.types);

        return returnType;
    }

    // addInTypesMap pour seulement modifier this.types sans besoin de garder le
    // type entré en parametre
    // comme pour les unification avec des accesseurs de tableau :
    // a = INT[][][] et b = #b
    // si on fait b = a[0][0] on appelle addInTypesMap(a[0][0].unify(b)) et on ne
    // veut pas garder le type de a[0][0] car on ne l'utilise pas,
    // on stocke deja le type de b et de a dans this.types. Ce qui est suffisant.
    public void addInTypesMap(Map<UnknownType, Type> modifMap) {
        addInTypesMap(modifMap, null);
    }

    // addInTypesMap permettant de ne pas avoir a creer une map pour ajouter un seul
    // element...
    public void addInTypesMap(UnknownType key, Type value) {
        Map<UnknownType, Type> map = new HashMap<UnknownType, Type>();
        map.put(key, value);
        addInTypesMap(map, null);
    }

    // verifie si le type contient une variable
    // c'est comme un contains mais pour n'importe quel UnknownType
    public boolean containsVar(Type t) {
        if (t instanceof UnknownType)
            return true;
        if (t instanceof ArrayType)
            return containsVar(((ArrayType) t).getTabType());
        return false;
    }

    // verifie si il y a une boucle infinie dans les types du genre : {#a=[][][]#a}
    // ne detecte pas petite boucle : { #a=[][]#a } car elle sont necessaire pour
    // les cas ou on transforme un tableau en tableau de tableau...
    public void verifBoucleInfini() {
        for (Entry<UnknownType, Type> entry : this.types.entrySet()) {
            if (entry.getValue().contains(entry.getKey()) && entry.getValue() instanceof ArrayType) {
                ArrayType next = (ArrayType) entry.getValue();

                for (int i = 0; i < 2; i++) {
                    if (next.getTabType() instanceof ArrayType) {
                        next = (ArrayType) next.getTabType();
                    } else {
                        return;
                    }
                }

                System.out.println("Boucle infinie : " + entry.getKey());
                System.out.println(this.types);
                throw new UnsupportedOperationException("Boucle infinie");
            }
        }
    }
}