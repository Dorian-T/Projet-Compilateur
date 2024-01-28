import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;


import javax.sound.sampled.Control;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Asm.Program;
import Asm.Ret;
import Asm.Stop;
import Asm.UAL;
import Asm.UALi;
import Asm.Com;
import Asm.CondJump;
import Asm.IO;
import Asm.Instruction;
import Asm.JumpCall;
import Asm.Mem;
import Asm.Label;
import Type.Type;
import Type.UnknownType;
import Type.Primitive_Type;
import grammarTCLParser.InstrContext;

/**
 * Classe CodeGenerator permettant de générer du code à partir d'un arbre syntaxique.
 * Elle étend AbstractParseTreeVisitor pour visiter l'arbre syntaxique et implémente
 * grammarTCLVisitor pour le traitement spécifique du langage TCL.
 */
public class CodeGenerator  extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    /**
     * Table de correspondance entre les types inconnus et leurs types réels.
     */
    private Map<String,Type> types;

    /**
     * Compteur pour générer des numéros de registre uniques.
     */
    private int registerCounter;

    /**
     * Table des variables, associant à chaque nom de variable son adresse.
     */
    private Map<String, Integer> variableTable;

    /**
     * Compteur pour générer des étiquettes uniques.
     */
    private int labelCounter;

    /**
     * Constructeur de CodeGenerator.
     * Initialise le générateur de code avec les types des variables.
     * 
     * @param types Map associant à chaque type inconnu son type réel.
     */
    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = new HashMap<>();
        for (Map.Entry<UnknownType, Type> entry : types.entrySet()) {
            this.types.put(entry.getKey().toString(), entry.getValue());
        }

        this.registerCounter = 3;
        this.variableTable = new java.util.HashMap<String, Integer>();
        this.labelCounter = 0;
    }

    /**
     * Affiche toutes les variables et le registre dans lequel elles sont.
     */
    public void afficherVariable() {
        System.out.println("Affichage des variables : ");
        for (Entry<String, Integer> entry : variableTable.entrySet()) {
            System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("Fin de l'affichage des variables");
    }

    /**
     * Génère un nouveau numéro de registre unique.
     * 
     * @return Le nouveau numéro de registre.
     */
    private int getNewRegister() {
        // Incrémente le compteur et renvoie le nouveau numéro de registre
        return registerCounter++;
    }

    /**
     * Récupère ou génère une adresse pour une variable donnée.
     * 
     * @param variableName Le nom de la variable.
     * @return L'adresse de la variable.
     */
    private int getVariableAddress(String variableName) {
        if (variableTable.containsKey(variableName)) {
            return variableTable.get(variableName);
        } else {
            int newAddress = generateNewAddress();
            variableTable.put(variableName, newAddress);
            return newAddress;
        }
    }
    
    /**
     * Génère une nouvelle adresse mémoire unique pour une variable.
     * 
     * @return La nouvelle adresse mémoire.
     */
    private int generateNewAddress() {
        return getNewRegister() + 1;
    }

    /**
     * Génère une nouvelle étiquette unique.
     * 
     * @return La nouvelle étiquette.
     */
    public String generateNewLabel() {
        return "LABEL_" + labelCounter++;
    }

    /**
     * Génère un nouveau label qu'on peut commenter
     * 
     * @param Commentaire Le type pour nommer l'étiquette.
     * @return La nouvelle étiquette.
     */
    public String generateNewLabel(String Commentaire) {
        return Commentaire + "_LABEL_" + labelCounter++;
    }

    /**
     * Récupère le type réel associé au nom d'une variable.
     * Cette méthode est utilisée pour obtenir le type d'une variable à partir de son nom.
     *
     * @param var  Le nom de la variable dont on souhaite obtenir le type.
     * @return     Le type réel associé à la variable, ou null si la variable n'est pas trouvée.
     */
    public Type getType(String var) {
        return types.get(var); // Retourne le type réel
    }

    /**
     * Visite un nœud de négation dans l'arbre syntaxique.
     * Génère le code pour nier une expression.
     * 
     * @param ctx Le contexte de négation.
     * @return Le programme généré pour la négation.
     */
    @Override
    public Program visitNegation(grammarTCLParser.NegationContext ctx) {
        System.out.println("visitNegation");
        Program program = new Program();
        
        // Évaluer l'expression à nier
        Program exprProgram = visit(ctx.expr());
        program.addInstructions(exprProgram);
        int exprReg = registerCounter - 1; // Le dernier registre utilisé contient le résultat de l'expression

        // Créer une étiquette pour le cas où l'expression est fausse (et donc la négation vraie)
        String trueLabel = generateNewLabel();
    
        // Si l'expression est fausse (0), sauter à trueLabel pour mettre le résultat à 1
        program.addInstruction(new CondJump(CondJump.Op.JEQU, exprReg, 0, trueLabel));
    
        // Si l'expression est vraie (pas 0),  préparer le registre de résultat pour qu'il passe à 0 (faux)
        program.addInstruction(new UALi(UALi.Op.SUB, exprReg, 0, 1));

        // Si l'expression est vraie (1)
        program.addInstruction(new UALi(trueLabel,UALi.Op.ADD,exprReg,exprReg,1)); // Mettre le résultat à 1 - resultReg
    
        return program;
    }
    
    /**
     * Visite un nœud de comparaison dans l'arbre syntaxique.
     * Génère le code pour comparer deux expressions.
     * 
     * @param ctx Le contexte de comparaison.
     * @return Le programme généré pour la comparaison.
     */
    @Override
    public Program visitComparison(grammarTCLParser.ComparisonContext ctx) {
        System.out.println("visitComparison");
        Program program = new Program();
        
        // Évaluer la première expression de la comparaison
        Program leftProgram = visit(ctx.expr(0));
        program.addInstructions(leftProgram);
        int leftReg = registerCounter - 1; // Le dernier registre utilisé contient le résultat de la première expression
        
        // Évaluer la seconde expression de la comparaison
        Program rightProgram = visit(ctx.expr(1));
        program.addInstructions(rightProgram);
        int rightReg = registerCounter - 1; // Le dernier registre utilisé contient le résultat de la seconde expression
    
        // Utiliser un nouveau registre à 0 pour le résultat de la comparaison
        int resultReg = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, resultReg, resultReg, resultReg));
    
        // Créer une étiquette pour le cas où la comparaison est vraie
        String trueLabel = generateNewLabel();

        switch(ctx.getChild(1).getText()) {
            case "<":
                program.addInstruction(new CondJump(CondJump.Op.JINF, leftReg, rightReg, trueLabel));
                break;
            case ">":
                program.addInstruction(new CondJump(CondJump.Op.JSUP, leftReg, rightReg, trueLabel));
                break;
            case "<=":
                program.addInstruction(new CondJump(CondJump.Op.JIEQ, leftReg, rightReg, trueLabel));
                break;
            case ">=":
                program.addInstruction(new CondJump(CondJump.Op.JSEQ, leftReg, rightReg, trueLabel));
                break;
            default:          
                break;
        }
        program.addInstruction(new UALi(UALi.Op.ADD, resultReg, 0, 1)); // Mettre le résultat à 1
        // Si les valeurs sont égales, on met le résultat à 1 (vrai) et on continue
        program.addInstruction(new UAL(trueLabel,UAL.Op.SUB, resultReg, 1, resultReg)); // On fait 1 - rslt
    
        return program;
    }

    /**
     * Visite un nœud de l'opération OR dans l'arbre syntaxique.
     * Génère le code pour effectuer une opération logique OR entre deux expressions.
     * 
     * @param ctx Le contexte de l'opération OR.
     * @return Le programme généré pour l'opération OR.
     */
    @Override
    public Program visitOr(grammarTCLParser.OrContext ctx) {
        System.out.println("visitOr");
        Program program = new Program();
        
        // Évaluer la première expression de l'opération OR
        Program leftProgram = visit(ctx.expr(0));
        program.addInstructions(leftProgram);
        int leftReg = registerCounter - 1; // Le registre contenant le résultat de la première expression
        
        // Évaluer la seconde expression de l'opération OR
        Program rightProgram = visit(ctx.expr(1));
        program.addInstructions(rightProgram);
        int rightReg = registerCounter - 1; // Le registre contenant le résultat de la seconde expression

        // Utiliser un nouveau registre pour le résultat de l'opération OR
        int resultReg = getNewRegister();
        
        // Initialiser le registre de résultat à 0 (faux)
        program.addInstruction(new UAL(UAL.Op.XOR, resultReg, resultReg, resultReg));

        // Créer des étiquettes pour la logique de l'opération OR
        String trueLabel = generateNewLabel();
        String continueLabel = generateNewLabel();

        // Si l'une des expressions est vraie, sauter à trueLabel
        program.addInstruction(new CondJump(CondJump.Op.JNEQ, leftReg, 0, trueLabel));
        program.addInstruction(new CondJump(CondJump.Op.JNEQ, rightReg, 0, trueLabel));

        // Si les deux expressions sont fausses, on saute à continueLabel, le résultat reste faux
        program.addInstruction(new JumpCall(JumpCall.Op.JMP, continueLabel));

        // Si l'une des expressions est vraie, on met le résultat à 1 (vrai)
        program.addInstruction(new Label(trueLabel));
        program.addInstruction(new UALi(UALi.Op.ADD, resultReg, 0, 1));

        // Continuer l'exécution après l'opération OR
        program.addInstruction(new Label(continueLabel));

        return program;
    }
 
    /**
     * Visite un nœud d'opposition dans l'arbre syntaxique.
     * Génère le code pour inverser le signe d'une expression.
     * 
     * @param ctx Le contexte d'opposition.
     * @return Le programme généré pour l'opposition.
     */
    @Override
    public Program visitOpposite(grammarTCLParser.OppositeContext ctx) {
        System.out.println("visitOpposite");
        Program program = new Program();
        
        // Évaluer l'expression dont on doit inverser le signe
        Program exprProgram = visit(ctx.expr());
        program.addInstructions(exprProgram);
        int exprReg = registerCounter - 1; // Le dernier registre utilisé contient le résultat de l'expression
        
        // Utiliser un nouveau registre pour le résultat de l'opération d'opposition
        int resultReg = getNewRegister();
    
        // Inverser le signe de l'expression en soustrayant sa valeur de 0
        program.addInstruction(new UAL(UAL.Op.SUB, resultReg, 0, exprReg)); // Résultat = 0 - exprReg
        //on a 0 dans le registre 0
    
        return program;
    }

    /**
     * Visite un nœud d'entier dans l'arbre syntaxique.
     * Génère le code pour traiter une valeur entière.
     *
     * @param ctx Le contexte du nœud d'entier.
     * @return Le programme généré pour le traitement de l'entier.
     */
    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        System.out.println("visitInteger");
        //on utilise les camps supp de Program pour stocker le type et la valeur
        Program program = new Program();
        int newReg = getNewRegister();

        program.addInstruction(new Com("Integer"));
        program.addInstruction(new UALi(UALi.Op.ADD, newReg, 0, Integer.parseInt(ctx.INT().getText())));

        return program;
    }

    /**
     * Visite un nœud d'accès à un tableau dans l'arbre syntaxique.
     * Génère le code pour accéder à une case spécifique d'un tableau.
     * 
     * @param ctx Le contexte d'accès au tableau.
     * @return Le programme généré pour l'accès au tableau.
     */
    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        System.out.println("visitTab_access");
        Program program = new Program();
        // recuperer registre nom tableau
        program.addInstruction(new Com("TabAccess"));
        program.addInstructions(visit(ctx.expr(0)));
        int tabAdress = registerCounter - 1;

        program.addInstructions(visit(ctx.expr(1)));
        int index = registerCounter - 1;

        //on fait un registre pour savoir à quelle case du tableau on est
        int caseTab = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, caseTab, caseTab, caseTab));

        int reg8 = getNewRegister();
        program.addInstruction(new UALi(UALi.Op.ADD, reg8, 0, 8));

        int tailleTab = getNewRegister();
        program.addInstruction(new Mem(Mem.Op.LD, tailleTab, tabAdress));
        program.addInstruction(new UALi(UALi.Op.ADD,tabAdress,tabAdress,1));//on prend le 1er element du tableau
        //on vérifie que l'index est bien dans le tableau
        String tailleTabOK = generateNewLabel("TailleTabOK_");

        program.addInstruction(new CondJump(CondJump.Op.JINF, index, tailleTab, tailleTabOK));
        
        //on affiche une erreur

        int newReg = getNewRegister();
        program.addInstruction(new UALi(UALi.Op.SUB, newReg, 0, 1));
        program.addInstruction(new IO(IO.Op.PRINT, newReg));
        program.addInstruction(new Stop());

        //on continue
        int adresseCase = getNewRegister();
        //index - caseTab = adresseCase
        program.addInstruction(new UAL(tailleTabOK,UAL.Op.SUB, adresseCase, index, caseTab));

        String dansBonneSection = generateNewLabel("SectionTabOK_");
        program.addInstruction(new CondJump(CondJump.Op.JINF, adresseCase, reg8, dansBonneSection));

        // on passe à la section suivante
        program.addInstruction(new UALi(UALi.Op.ADD, tabAdress, tabAdress, 8));
        program.addInstruction(new Mem(Mem.Op.LD, tabAdress, tabAdress));
        program.addInstruction(new UALi(UALi.Op.ADD, caseTab, caseTab, 8));
        program.addInstruction(new JumpCall(JumpCall.Op.JMP, tailleTabOK));

        //on est dans la bonne section
        program.addInstruction(new UAL(dansBonneSection,UAL.Op.ADD, tabAdress, tabAdress, adresseCase));
        //on ajoute ce qui manque pour arriver à la bonne case

        program.addInstruction(new Com("Retour tab access:"));
        //on charge la valeur de la case dans le registre
        int rslt = getNewRegister();
        program.addInstruction(new Mem(Mem.Op.LD, rslt, tabAdress));

        return program;
    }

    /**
     * Visite un nœud contenant des expressions entre crochets dans l'arbre syntaxique.
     * Génère le code pour évaluer l'expression entre crochets.
     * 
     * @param ctx Le contexte des crochets.
     * @return Le programme généré pour l'expression entre crochets.
     */
    @Override
    public Program visitBrackets(grammarTCLParser.BracketsContext ctx) {
        Program program = new Program();

        // Visite l'expression contenue entre les crochets
        Program exprProgram = visit(ctx.expr());

        // Ajoute les instructions générées pour l'expression entre crochets
        program.addInstructions(exprProgram);

        return program;
    }

    /**
     * Visite un nœud d'appel de fonction dans l'arbre syntaxique.
     * Génère le code pour appeler une fonction et gérer ses arguments.
     * 
     * @param ctx Le contexte de l'appel de fonction.
     * @return Le programme généré pour l'appel de fonction.
     */
    @Override
    public Program visitCall(grammarTCLParser.CallContext ctx) {
        System.out.println("visitCall");
        Program program = new Program();
    

        for (int i = 0; i < ctx.expr().size(); i++) {
            // Visite chaque argument de la fonction
            System.out.println("\t" + ctx.expr(i).getText() + " dans " + getVariableAddress(ctx.expr(i).getText()));
            Program argProgram = visit(ctx.expr(i));
            program.addInstructions(argProgram);
            //on les empiles, après avoir augmenté le stack pointer
            program.addInstruction(new UALi(UALi.Op.ADD, 2,2,1));
            program.addInstruction(new Mem(Mem.Op.ST, registerCounter - 1, 2));
        }   

        // Récupère le nom de la fonction depuis le contexte
        String functionName = ctx.VAR().getText();
        //on ajoute un Call
        program.addInstruction(new JumpCall(JumpCall.Op.CALL, functionName));

        //on récupère le résultat de la fonction
        int newReg = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, newReg, newReg, newReg));
        program.addInstruction(new Mem(Mem.Op.LD, newReg, 2));
        program.addInstruction(new UALi(UALi.Op.SUB, 2,2,1));

        return program;
    }
    
    /**
     * Visite un nœud de valeur booléenne dans l'arbre syntaxique.
     * Génère le code pour représenter une valeur booléenne (true ou false).
     * 
     * @param ctx Le contexte de la valeur booléenne.
     * @return Le programme généré pour la valeur booléenne.
     */
    @Override
    public Program visitBoolean(grammarTCLParser.BooleanContext ctx) {
        System.out.println("visitBoolean");
        //on utilise les camps supp de Program pour stocker le type et la valeur
        Program program = new Program();
        int newReg = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, newReg, newReg, newReg));

        if (ctx.BOOL().getText().equals("true"))
            program.addInstruction(new UALi(UALi.Op.ADD, newReg, newReg, 1));

        // c'est un INT certe, mais normalement les erreures ont étés gérées avant
        return program;
    }

    /**
     * Visite un nœud de l'opération AND dans l'arbre syntaxique.
     * Génère le code pour effectuer une opération logique AND entre deux expressions.
     * 
     * @param ctx Le contexte de l'opération AND.
     * @return Le programme généré pour l'opération AND.
     */
    @Override
    public Program visitAnd(grammarTCLParser.AndContext ctx) {
        System.out.println("visitAnd");
        Program program = new Program();
        
        // Évaluer la première expression de l'opération AND
        Program leftProgram = visit(ctx.expr(0));
        program.addInstructions(leftProgram);
        int leftReg = registerCounter - 1; // Le registre contenant le résultat de la première expression
        
        // Évaluer la seconde expression de l'opération AND
        Program rightProgram = visit(ctx.expr(1));
        program.addInstructions(rightProgram);
        int rightReg = registerCounter - 1; // Le registre contenant le résultat de la seconde expression

        // Utiliser un nouveau registre initialisé à 0 pour le résultat de l'opération AND
        int resultReg = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, resultReg, resultReg, resultReg));
    
       // Créer une étiquette pour la suite du code après l'opération AND
        String continueLabel = generateNewLabel();

        //si l'une des expressions est fausse, on saute à continueLabel (donc rslt = 0)
        program.addInstruction(new CondJump(CondJump.Op.JEQU, leftReg, 0, continueLabel));
        program.addInstruction(new CondJump(CondJump.Op.JEQU, rightReg, 0, continueLabel));
        // Si les deux expressions sont vraies, on met le résultat à 1 (vrai)
        program.addInstruction(new UALi(UALi.Op.ADD, resultReg, resultReg, 1)); // Mettre le résultat à 1
        program.addInstruction(new Label(continueLabel));
    
        return program;
    }
    
    /**
     * Visite un nœud de variable dans l'arbre syntaxique.
     * Génère le code pour accéder à une variable.
     * 
     * @param ctx Le contexte de la variable.
     * @return Le programme généré pour l'accès à la variable.
     */
    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        System.out.println("visitVariable");
        Program program = new Program();
    
        // Récupérer le nom de la variable depuis le contexte
        String varName = ctx.getText();
    
        // Récupérer l'adresse de la variable
        int varAddress = getVariableAddress(varName);
    
        // Copier la valeur de la variable dans un nouveau registre
        int newReg = getNewRegister();
        program.addInstruction(new UALi(UALi.Op.ADD, newReg, varAddress, 0));
    
        return program;
    }
    
    /**
     * Visite un nœud de multiplication dans l'arbre syntaxique.
     * Génère le code pour multiplier, diviser ou effectuer un modulo entre deux expressions.
     * 
     * @param ctx Le contexte de multiplication.
     * @return Le programme généré pour l'opération.
     */
    @Override
    public Program visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        System.out.println("visitMultiplication");
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à multiplier
        Program leftExprProgram = visit(ctx.expr(0));
        int sr1 = registerCounter - 1;
        Program rightExprProgram = visit(ctx.expr(1));
        int sr2 = registerCounter - 1;

        // Génère le code assembleur pour l'opération de multiplication
        int destRegister = getNewRegister();
        program.addInstructions(leftExprProgram);
        program.addInstructions(rightExprProgram);
        switch(ctx.getChild(1).getText()) {
            case "*":
                program.addInstruction(new UAL(UAL.Op.MUL, destRegister, sr1, sr2)); // Multiplication
                break;
            case "/":
                program.addInstruction(new UAL(UAL.Op.DIV, destRegister, sr1, sr2)); // Division
                break;
            case "%":
                program.addInstruction(new UAL(UAL.Op.MOD, destRegister, sr1, sr2)); // Modulo
                break;
        }
        return program;
    }

    /**
     * Visite un nœud d'égalité dans l'arbre syntaxique.
     * Génère le code pour comparer l'égalité ou l'inégalité entre deux expressions.
     * 
     * @param ctx Le contexte d'égalité.
     * @return Le programme généré pour la comparaison.
     */
    @Override
    public Program visitEquality(grammarTCLParser.EqualityContext ctx) {
        System.out.println("visitEquality");
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expr(0));
        program.addInstructions(leftExprProgram);
        int sr1 = registerCounter - 1;
        //droite
        Program rightExprProgram = visit(ctx.expr(1));
        program.addInstructions(rightExprProgram);
        int sr2 = registerCounter - 1;

        
        // Génère le code assembleur pour l'opération d'égalité
        int destRegister = getNewRegister();
        Label trueLabel = new Label(generateNewLabel());
        //on met 0 dans le registre de destination
        program.addInstruction(new UAL(UAL.Op.XOR, destRegister, 0, 0));

        switch (ctx.getChild(1).getText()) {
            case "==":                
                // Sauter à TrueLabel si les deux valeurs sont égales
                program.addInstruction(new CondJump(CondJump.Op.JEQU, sr1, sr2, trueLabel.getLabel())); 
                break;
            case "!=":
                // Sauter à TrueLabel si les deux valeurs sont inégales
                program.addInstruction(new CondJump(CondJump.Op.JNEQ, sr1, sr2, trueLabel.getLabel())); 
                break;
            default:
                throw new RuntimeException("Opérateur d'égalité non reconnu : " + ctx.getChild(1).getText());
        }

        //si le rslt est faux, on met 1 dans le registre de destination
        program.addInstruction(new UALi(UALi.Op.ADD, destRegister, 0, 1));

        //Enfin, on inverse le résultat, donc 1 si c'est bon, 0 sinon : op : 1 - destRegister[=0 ou 1]
        program.addInstruction(new UAL(trueLabel.getLabel(),UAL.Op.SUB, destRegister, 1, destRegister));

        return program;
    }

    /**
     * Visite un nœud d'initialisation de tableau dans l'arbre syntaxique.
     * Génère le code pour initialiser un tableau.
     * 
     * @param ctx Le contexte d'initialisation du tableau.
     * @return Le programme généré pour l'initialisation du tableau.
     */
    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        System.out.println("visitTab_initialization");

        Program program = new Program();
        program.addInstruction(new Com("TabInitialization"));

        int tabAdress = registerCounter - 1;

        int tabIterator = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.ADD, tabIterator, 0, tabAdress));
        int size = getNewRegister();
        program.addInstruction(new UALi(UALi.Op.ADD, size, 0, ctx.expr().size()));
        program.addInstruction(new Mem(Mem.Op.ST, size, tabIterator));
        
        for (int i = 0; i < ctx.expr().size(); i++) {
            
            //on passe à la case suivante du tableau
            program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, tabIterator, 1));

            if (i != 0 && i % 8 == 0)
            {

                // on prend l'adresse de la première case libre de la memoire
                program.addInstruction(new UALi(UALi.Op.ADD, 2,2,1));
                //on met l'adresse de la prochaine partie du tableau dans la section précédente
                program.addInstruction(new Mem(Mem.Op.ST, 2, tabIterator));  
 
                // mettre stack pointer dans l'itérateur du tableau
                program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, 2, 0));
                program.addInstruction(new UALi(UALi.Op.ADD, 2, 2, 8));//8 cases dans le tableau, on passe de case 1 à case 9 (prochaine ad)
            }
            // Visite les expressions à ajouter au tableau
            Program argProgram = visit(ctx.expr(i));
            program.addInstructions(argProgram);
            program.addInstruction(new Mem(Mem.Op.ST, registerCounter - 1, tabIterator));
        }

        //renvois l'adresse du tableau
        program.addInstruction(new UAL(UAL.Op.ADD, registerCounter - 1, 0, tabAdress));
        //normalement ça casse rien


        program.addInstruction(new Com("End Tab Init"));
        return program;
    }
    
    /**
     * Visite un nœud d'addition dans l'arbre syntaxique.
     * Génère le code pour effectuer une addition ou une soustraction entre deux expressions.
     *
     * @param ctx Le contexte d'addition.
     * @return Le programme généré pour l'opération d'addition ou de soustraction.
     */
    @Override
    public Program visitAddition(grammarTCLParser.AdditionContext ctx) {
        System.out.println("visitAddition");
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à additionner
        Program leftExprProgram = visit(ctx.expr(0));
        int sr1 = registerCounter - 1;

        Program rightExprProgram = visit(ctx.expr(1));
        int sr2 = registerCounter - 1;

        // Ajoute les instructions générées pour les expressions à additionner
        program.addInstructions(leftExprProgram);
        program.addInstructions(rightExprProgram);

        // Génère le code pour l'opération d'addition
        int destRegister = getNewRegister();
        switch(ctx.getChild(1).getText()) {
            case "+":
                program.addInstruction(new UAL(UAL.Op.ADD, destRegister, sr1, sr2)); // Addition
                break;
            case "-":
                program.addInstruction(new UAL(UAL.Op.SUB, destRegister, sr1, sr2)); // Soustraction
                break;
            default:
                break;
        }

        return program;
    }

    /**
     * Visite un nœud de déclaration de type de base dans l'arbre syntaxique.
     * Génère le code pour déclarer un type de base (int, bool, etc.).
     * 
     * @param ctx Le contexte de déclaration de type de base.
     * @return Le programme généré pour la déclaration de type de base.
     */
    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        System.out.println("visitBase_type");
        Program program = new Program();

        // Génère le code pour la déclaration du type de base
        program.addInstruction(new Com("BaseType"));

        //c'est soit un int soit un bool, ou un auto...

        return program;
    }

    /**
     * Visite un nœud de déclaration de type de tableau dans l'arbre syntaxique.
     * Génère le code pour déclarer un type de tableau.
     * 
     * @param ctx Le contexte de déclaration de type de tableau.
     * @return Le programme généré pour la déclaration de type de tableau.
     */
    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        System.out.println("visitTab_type");

        Program program = new Program();
        program.addInstruction(new Com("TabType"));
        program.addInstructions(visit(ctx.type()));
        
        //on assigne des cases mémoires pour le tableau
        int newReg = getNewRegister();
        //incrémentation du stack pointer
        program.addInstruction(new UALi(UALi.Op.ADD, 2,2,1));
        //on charge l'adresse du tableau dans le registre
        program.addInstruction(new UALi(UALi.Op.ADD, newReg, 2, 0));
        //on met la taille du tableau dans le registre
        program.addInstruction(new Mem(Mem.Op.ST, 0, 2));

        //on place le tableau dans la mémoire : 
        program.addInstruction(new UALi(UALi.Op.ADD, 2, 2, 9));
        //on initialise l'adresse de la prochaine partie du tableau à 0
        program.addInstruction(new Mem(Mem.Op.ST, 0, 2));

        //on met l'adresse du tableau dans le registre
        int tabAdress = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.ADD, tabAdress, newReg, 0));
        
        return program;
    }

    /**
     * Visite un nœud de déclaration de variable dans l'arbre syntaxique.
     * Génère le code pour déclarer une variable.
     * 
     * @param ctx Le contexte de déclaration de variable.
     * @return Le programme généré pour la déclaration de variable.
     */
    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        System.out.println("visitDeclaration");
        Program program = new Program();
        /* !!!prob : on ne peut avoir qu'une déclaration d'entier (ou de booléen des fois)
        */
        int destRegister = getNewRegister();
        // on déclare la variable dans la table des variables
        variableTable.put(ctx.VAR().getText(), destRegister);

        //on gère les tableaux 
        if (getType(ctx.VAR().getText()) instanceof Primitive_Type) {
            //on met 0 dans le registre
            program.addInstruction(new UAL(UAL.Op.XOR, destRegister, 0, 0));
            
        }
        else // tableaux
        {
            //on gère les tableaux (visit le type) et retourne l'adresse du tableau
            program.addInstructions(visit(ctx.type()));
            //on met l'adresse du tableau dans le registre
            program.addInstruction(new UAL(UAL.Op.ADD, destRegister, registerCounter - 1, 0));
        }

        if (ctx.expr() == null) return program;
        // si on a une expression , on doit l'évaluer et mettre le résultat dans la variable
        
        program.addInstructions(visit(ctx.expr()));
       
        // on déclare la variable : init à 0 puis on la modifie avec l'expression
        program.addInstruction(new UALi(UALi.Op.ADD, destRegister, registerCounter - 1, 0));
        return program;
    }
     
    /**
     * Visite un nœud d'instruction print dans l'arbre syntaxique.
     * Génère le code pour imprimer une valeur.
     * 
     * @param ctx Le contexte de l'instruction print.
     * @return Le programme généré pour l'instruction print.
     */
    @Override
    public Program visitPrint(grammarTCLParser.PrintContext ctx) {
        System.out.println("visitPrint");
        Program program = new Program();
        
        // Récupérer le nom de la variable à imprimer
        String varName = ctx.VAR().getText();
        
        afficherVariable();

        // Récupérer le registre ou l'adresse associée à la variable
        int varAddress = variableTable.get(varName);

        program.addInstruction(new IO(IO.Op.PRINT, varAddress));

        return program;
    }
    
    /**
     * Visite un nœud d'assignation dans l'arbre syntaxique.
     * Génère le code pour assigner une valeur à une variable.
     * 
     * @param ctx Le contexte d'assignation.
     * @return Le programme généré pour l'assignation.
     */
    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        System.out.println("visitAssignment");
        Program program = new Program();
        program.addInstruction(new Com("Assignment"));

        // trouve la valeur à assigner en visitan expression dans TYPE[trucs] = expression 
        Program expressionProgram = visit(ctx.expr(ctx.expr().size() - 1));
        program.addInstructions(expressionProgram);
        // Enregistre le registre contenant le résultat de l'expression
        int source = registerCounter - 1;

        //source est donc le registre contenant le résultat de l'expression
        int regVarAssignee = getVariableAddress(ctx.VAR().getText());

        //cas variable simple
        if (getType(ctx.VAR().getText()) instanceof Primitive_Type) {
            //on assigne la valeur de l'expression à la variable
            program.addInstruction(new UAL(UAL.Op.ADD, regVarAssignee, source, 0));
            program.addInstruction(new Com("End Assignment"));
            return program;
        }
        //cas tableau --------------------------------------------------------------------------------------

        int reg8 = getNewRegister();
        program.addInstruction(new UALi(UALi.Op.ADD, reg8, 0, 8));

        //si c'est un tableau, on doit faire un truc spécial :
        //c'est comme à la fête des mères quoi...
        int tabAdress = regVarAssignee;
        //on entre dans l'accès au tableau (initialisation des variables)
        int tabIterator = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.ADD, tabIterator, tabAdress, 0));
        for (int i= 0; i < ctx.expr().size() - 1; i++) { 
            
            // Visite chaque argument de la fonction (dans l'ordre)
            program.addInstructions(visit(ctx.expr(i)));
            //récupère l'index de la case du tableau
            int index = registerCounter - 1;
            int NbSectionIndex = getNewRegister();
            int reste = getNewRegister();
    
            //récupère la taille du tableau
            int nbSectionTab = getNewRegister();
            int realTailleTab = getNewRegister();
            program.addInstruction(new Mem(Mem.Op.LD, realTailleTab, tabIterator));
            // ------------------------- on met à jour la taille du tableau si besoin -------------------------
                program.addInstruction(new UALi(UALi.Op.ADD, reste, index, 1));
                String truc = generateNewLabel("truc_");
                program.addInstruction(new CondJump(CondJump.Op.JSEQ, realTailleTab, reste,truc));
                program.addInstruction(new Mem(Mem.Op.ST, reste, tabIterator));
            program.addInstruction(new Label(truc));

            program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, tabIterator, 1));
            program.addInstruction(new UALi(UALi.Op.ADD, nbSectionTab, realTailleTab, 7));
            //si c'est 0, passe à 1 pour éviter la division par 0
            String taille0 = generateNewLabel("Taille0_");
            program.addInstruction(new CondJump(CondJump.Op.JNEQ, realTailleTab, 0, taille0));
            program.addInstruction(new UALi(UALi.Op.ADD, nbSectionTab, realTailleTab, 1));
            program.addInstruction(new Label(taille0));
            program.addInstruction(new UALi(UALi.Op.DIV, nbSectionTab, nbSectionTab, 8));
            //on a le nombre de cases mémoires réservées pour le tableau

            program.addInstruction(new UALi(UALi.Op.ADD, reste, index, 8));
            program.addInstruction(new UALi(UALi.Op.DIV, NbSectionIndex, reste, 8));
            //on a le nombre de cases mémoires à réserver pour le tableau

            //si le nombre de sections de l'index est supérieur au nombre de sections du tableau, on augmente la taille du tableau
            String tabTropPetit = generateNewLabel("tab_trop_petit");
            String TailleTabOK = generateNewLabel("TailleTabOK_");

            program.addInstruction(new CondJump(CondJump.Op.JINF, nbSectionTab, NbSectionIndex, tabTropPetit)); //--------------------------------
            //yes sir
            // --------le tableau est assez grand, on peut continuer et on va à la bonne section --------------

            program.addInstruction(new UAL(UAL.Op.ADD, reste, NbSectionIndex, 0));
                String itererDansTab2 = generateNewLabel("ItererDansTab2_");
                program.addInstruction(new Label(itererDansTab2));
                    program.addInstruction(new UALi(UALi.Op.SUB, reste, reste, 1));
                    program.addInstruction(new CondJump(CondJump.Op.JEQU, reste, 0, TailleTabOK));
                    program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, tabIterator, 8));
                    program.addInstruction(new Mem(Mem.Op.LD, tabIterator, tabIterator));

                    program.addInstruction(new JumpCall(JumpCall.Op.JMP, itererDansTab2));
            
            // ---------------------------- on va à la dernière section déclarée du tableau - ----------------------------
            program.addInstruction(new Label(tabTropPetit));
            String tailleTabPasOK = generateNewLabel("TailleTabPasOK_");
            program.addInstruction(new UAL(UAL.Op.ADD, reste, nbSectionTab, 0));
            String itererDansTab = generateNewLabel("ItererDansTab_");
            program.addInstruction(new Label(itererDansTab));
                program.addInstruction(new UALi(UALi.Op.SUB, reste, reste, 1));
                program.addInstruction(new CondJump(CondJump.Op.JEQU, reste, 0, tailleTabPasOK));
                program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, tabIterator, 8));
                program.addInstruction(new Mem(Mem.Op.LD, tabIterator, tabIterator));
                
                program.addInstruction(new JumpCall(JumpCall.Op.JMP, itererDansTab));
            
            // ---------------------------- on déclare les cases comme il faut (au moins 1 passage) - ----------------------------
                program.addInstruction(new Label(tailleTabPasOK));
            
                //on augmente la taille du tableau
                program.addInstruction(new UALi(UALi.Op.ADD, 2,2,1)); //ahut pile
                program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, tabIterator, 8));//on passe à la case suivante
                program.addInstruction(new Mem(Mem.Op.ST, 2, tabIterator));  //enregistre l'adresse de la prochaine partie du tableau
                //on itère sur le tableau
                program.addInstruction(new UALi(UALi.Op.ADD, tabIterator, 2, 0));//on met a jours l'itérateur

                program.addInstruction(new UALi(UALi.Op.ADD, 2, 2, 8));//obn met à jour le stack pointer
      
                program.addInstruction(new UALi(UALi.Op.ADD, nbSectionTab, nbSectionTab, 1));//il y a une section de plus
                program.addInstruction(new CondJump(CondJump.Op.JINF, nbSectionTab, NbSectionIndex, tailleTabPasOK));
    
            // ------------ on est dans la bonne section, on peut prendre l'adresse de la bonne case ------------
            program.addInstruction(new Label(TailleTabOK));
            program.addInstruction(new UALi(UALi.Op.SUB, reste, NbSectionIndex, 1));
            program.addInstruction(new UALi(UALi.Op.MUL, reste, reste, 8));

            program.addInstruction(new UAL(UAL.Op.SUB, reste, index, reste));

            program.addInstruction(new UAL(UAL.Op.ADD, tabIterator, tabIterator, reste));

        }

        //on assigne la valeur de l'expression à la case du tableau
        program.addInstruction(new Mem(Mem.Op.ST, source, tabIterator));
        

        program.addInstruction(new Com("End Assignment"));
        return program;
    }

    /**
     * Visite un nœud de bloc d'instructions dans l'arbre syntaxique.
     * Génère le code pour un bloc d'instructions.
     * 
     * @param ctx Le contexte du bloc d'instructions.
     * @return Le programme généré pour le bloc d'instructions.
     */
    @Override
    public Program visitBlock(grammarTCLParser.BlockContext ctx) {
        System.out.println("visitBlock");
        Program program = new Program();

        // Itérer sur chaque instruction dans le bloc
        for (grammarTCLParser.InstrContext instruction : ctx.instr()) {
            // Visiter chaque instruction pour générer le code correspondant
            Program instructionProgram = visit(instruction);
            // Ajouter le programme généré par l'instruction au programme global du bloc
            program.addInstructions(instructionProgram);
        }

        return program;
    }

    /**
     * Visite un nœud d'instruction if dans l'arbre syntaxique.
     * Génère le code pour une instruction conditionnelle if.
     * 
     * @param ctx Le contexte de l'instruction if.
     * @return Le programme généré pour l'instruction if.
     */
    @Override
    public Program visitIf(grammarTCLParser.IfContext ctx) {
        System.out.println("visitIf");
        Program program = new Program();

        // Génère le code pour l'expression conditionnelle
        Program conditionProgram = visit(ctx.expr());
        program.addInstructions(conditionProgram);

        String Label1;
        if (ctx.instr().size() > 1) {
            Label1 = generateNewLabel("Else_");
        }
        else {
            Label1 = generateNewLabel("EndIf_");
        }

        //si la condition est != 0 donc false
        program.addInstruction(new CondJump(CondJump.Op.JEQU, registerCounter - 1, 0, Label1));
        
        //cas vrai
        Program trueBlockProgram = visit(ctx.instr(0));
        program.addInstructions(trueBlockProgram);

        // Si un bloc else existe, génère le code pour le bloc else
        if (ctx.instr().size() > 1) {
            System.out.println("\twith else");

            //cas true, on sort du if
            String Label2 = generateNewLabel("EndIf_");
            program.addInstruction(new JumpCall(JumpCall.Op.JMP, Label2));

            //cas false, on fait le else
            program.addInstruction(new Label(Label1));
            Program falseBlockProgram = visit(ctx.instr(1)); 
            program.addInstructions(falseBlockProgram);

            //porte de sortie du if
            program.addInstruction(new Label(Label2));
        }
        else {
            //pas de else, on sort du if
            program.addInstruction(new Label(Label1));
        }

        return program;
    }

    /**
     * Visite un nœud de boucle while dans l'arbre syntaxique.
     * Génère le code pour une boucle while avec une condition et un corps de boucle.
     * 
     * @param ctx Le contexte de la boucle while.
     * @return Le programme généré pour la boucle while.
     */
    @Override
    public Program visitWhile(grammarTCLParser.WhileContext ctx) {
        System.out.println("visitWhile");
        Program program = new Program();

        // Création de l'étiquette pour le début de la boucle while
        String startLabel = generateNewLabel("StartWhile");
        program.addInstruction(new Label(startLabel));

        // Générer le code pour l'expression conditionnelle
        Program conditionProgram = visit(ctx.expr());
        program.addInstructions(conditionProgram);

        // Création de l'étiquette pour la sortie de la boucle
        String endLabel = generateNewLabel("EndWhile");
        
        // Condition pour sortir de la boucle si elle n'est pas remplie
        program.addInstruction(new CondJump(CondJump.Op.JEQU, registerCounter - 1, 0, endLabel));

        // Générer le code pour le corps de la boucle
        Program bodyProgram = visit(ctx.instr());
        program.addInstructions(bodyProgram);

        // Sauter au début de la boucle
        program.addInstruction(new JumpCall(JumpCall.Op.JMP, startLabel));

        // Étiquette de fin de la boucle
        program.addInstruction(new Label(endLabel));

        return program;
    }
    
    /**
     * Visite un nœud de boucle for dans l'arbre syntaxique.
     * Génère le code pour une boucle for avec une initialisation, une condition, une itération et un corps de boucle.
     * 
     * @param ctx Le contexte de la boucle for.
     * @return Le programme généré pour la boucle for.
     */
    @Override
    public Program visitFor(grammarTCLParser.ForContext ctx) {
        System.out.println("visitFor");

        Program program = new Program();

        //for : instr(0) = init, expr() = condition, instr(1) = itération, instr(2) = corps
        //affichage pour vérifier

        // --- initialisation de la boucle ---------------------------

        // Supposons que la première instruction dans 'instr()' est l'initialisation
        Program initProgram = visit(ctx.instr(0));
        program.addInstructions(initProgram);
    
        // Étiquette pour le début de la boucle
        String startLabel = generateNewLabel("Dbt_For");
        program.addInstruction(new Label(startLabel));
    

        // -------------- Condition de la boucle ------------------------
        Program conditionProgram = visit(ctx.expr());
        program.addInstructions(conditionProgram);
    
        // Étiquette pour la fin de la boucle
        String endLabel = generateNewLabel("Fin_For");
        //si la condition passe à false (0), on jmp
        program.addInstruction(new CondJump(CondJump.Op.JEQU, registerCounter - 1, 0, endLabel));
    
        // Corps de la boucle (supposons que c'est la 3ième) --------------------
        Program bodyProgram = visit(ctx.instr(2));
        program.addInstructions(bodyProgram);

        // Supposons que la 2nd instruction est __l'itération__ ---------------------
        Program iterationProgram = visit(ctx.instr(1));
        program.addInstructions(iterationProgram);
    
        // Sauter au début de la boucle
        program.addInstruction(new JumpCall(JumpCall.Op.JMP, startLabel));
    
        // Étiquette de fin
        program.addInstruction(new Label(endLabel));
    
        return program;
    }

    /**
     * Visite un nœud de retour (return) dans l'arbre syntaxique.
     * Génère le code pour retourner une valeur depuis une fonction.
     * 
     * @param ctx Le contexte de retour.
     * @return Le programme généré pour l'instruction de retour.
     */
    @Override
    public Program visitReturn(grammarTCLParser.ReturnContext ctx) {
        System.out.println("visitReturn");

        // Créer un programme pour la valeur de retour
        Program returnProgram = visit(ctx.expr());
        // visit(expr) pose le résultat dans le dernier registre utilisé
        // c'est pourquoi on assigne pas de nouveaux registres
        //Store le résultat
        returnProgram.addInstruction(new UALi(UALi.Op.ADD, 2, 2, 1));
        returnProgram.addInstruction(new Mem(Mem.Op.ST, registerCounter - 1, 2));
        returnProgram.addInstruction(new Ret());
         
        return returnProgram;
    }

    /**
     * Visite un nœud de fonction de base dans l'arbre syntaxique.
     * Génère le code pour une fonction de base.
     * 
     * @param ctx Le contexte de la fonction de base.
     * @return Le programme généré pour la fonction de base.
     */
    @Override
    public Program visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        Program program = new Program();
        System.out.println("visitCore_fct");
        int i=0;
        String str = "";
        // Générer le code pour chaque instruction dans la fonction de base
        for (grammarTCLParser.InstrContext instrContext : ctx.instr()) {
            i+=1;
            str += "\tin fct : " + instrContext.getText() + "\n";
            Program instrProgram = visit(instrContext);
            program.addInstructions(instrProgram);
        }
        System.out.println(str);

        afficherVariable();

        // on gère le return
        if (ctx.getChild(i+1).getText().equals("return")) {
            program.addInstructions(visit(ctx.getChild(i+2)));
            program.addInstruction(new UALi(UALi.Op.ADD, 2, 2, 1));
            program.addInstruction(new Mem(Mem.Op.ST, registerCounter - 1, 2));
            program.addInstruction(new Ret());
        }

        return program;
    }

    /**
     * Visite un nœud de déclaration de fonction dans l'arbre syntaxique.
     * Génère le code pour déclarer une fonction.
     * 
     * @param ctx Le contexte de la déclaration de fonction.
     * @return Le programme généré pour la déclaration de fonction.
     */
    @Override
    public Program visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        Program program = new Program();
        //on affiche toutes les déclarations de fonctions
        System.out.println("visitDecl_fct");

        System.out.println("\t" + ctx.VAR(0).getText() + " : " + ctx.core_fct().getText());


        // On fait son étiquette
        String functionName = ctx.VAR(0).getText(); 
        program.addInstruction(new Label(functionName));

        for (int i =  ctx.VAR().size() -1; i > 0 ; i--) {
            System.out.println("arg : " + ctx.VAR(i).getText());

            //on ajoute les arguments dans la table des variables
            int newReg = getNewRegister();
            variableTable.put(ctx.VAR(i).getText(), newReg);
            //on met le type de la variable dans la hashmap

            //on empile l'argument
            program.addInstruction(new Mem(Mem.Op.LD, newReg, 2));
            program.addInstruction(new UALi(UALi.Op.SUB, 2,2,1));
        }

        // Générer le code pour le corps de la fonction
        Program coreFunctionProgram = visit(ctx.core_fct());
        program.addInstructions(coreFunctionProgram);

        // Ajouter l'instruction RET
        program.addInstruction(new Ret());

        return program;
    }    

    /**
     * Visite un nœud de fonction principale dans l'arbre syntaxique.
     * Génère le code pour la fonction principale.
     * 
     * @param ctx Le contexte de la fonction principale.
     * @return Le programme généré pour la fonction principale.
     */
    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        Program program = new Program();
        System.out.println("visitMain");
        program.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
        program.addInstruction(new UALi(UALi.Op.ADD, 1, 0, 1));
        program.addInstruction(new UAL(UAL.Op.XOR, 2, 2, 2));

        program.addInstruction(new JumpCall(JumpCall.Op.CALL, "main"));
        program.addInstruction(new Stop());

        program.addInstruction(new Label("main"));
        // Générer le code pour le corps de la fonction principale
        Program coreFunctionProgram = visitCore_fct(ctx.core_fct());
        program.addInstructions(coreFunctionProgram);

        // Générer le code pour chaque déclaration de fonction
        for (grammarTCLParser.Decl_fctContext declContext : ctx.decl_fct()) {
            Program declProgram = visitDecl_fct(declContext);
            program.addInstructions(declProgram);
        }
        return program;
    }
}
