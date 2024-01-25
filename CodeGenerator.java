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
import Asm.CondJump;
import Asm.IO;
import Asm.Instruction;
import Asm.JumpCall;
import Asm.Mem;
import Asm.Label;
import Type.Type;
import Type.UnknownType;
import grammarTCLParser.InstrContext;

public class CodeGenerator  extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    private Map<UnknownType,Type> types;
    private int registerCounter;
    private Map<String, Integer> variableTable;
    private int labelCounter;

    /**
     * Constructeur
     * @param types types de chaque variable du code source
     */
    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
        this.registerCounter = 3;
        this.variableTable = new java.util.HashMap<String, Integer>();
        this.labelCounter = 0;

    }

    public void afficherVariable() {
        System.out.println("Affichage des variables : ");
        for (Entry<String, Integer> entry : variableTable.entrySet()) {
            System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("Fin de l'affichage des variables");
    }
    /**
     * Obtient un nouveau numéro de registre disponible.
     * @return Un nouveau numéro de registre
     */
    private int getNewRegister() {
        // Incrémente le compteur et renvoie le nouveau numéro de registre
        return registerCounter++;
    }

    private int getVariableAddress(String variableName) {
        if (variableTable.containsKey(variableName)) {
            return variableTable.get(variableName);
        } else {
            int newAddress = generateNewAddress();
            variableTable.put(variableName, newAddress);
            return newAddress;
        }
    }
    
    private int generateNewAddress() {
        return variableTable.size() + 1;
    }

    public String generateNewLabel() {
        return "LABEL_" + labelCounter++;
    }
    public String generateNewLabel(String type) {
        return type + "_LABEL_" + labelCounter++;
    }
    
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

    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        System.out.println("visitInteger");
        //on utilise les camps supp de Program pour stocker le type et la valeur
        Program program = new Program();
        int newReg = getNewRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, newReg, newReg, newReg));
        program.addInstruction(new UALi(UALi.Op.ADD, newReg, newReg, Integer.parseInt(ctx.INT().getText())));

        return program;
    }

    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_access'");
    }

    @Override
    public Program visitBrackets(grammarTCLParser.BracketsContext ctx) {
        Program program = new Program();

        // Visite l'expression contenue entre les crochets
        Program exprProgram = visit(ctx.expr());

        // Ajoute les instructions générées pour l'expression entre crochets
        program.addInstructions(exprProgram);

        return program;
    }

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
    
    
    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        System.out.println("visitVariable");
        Program program = new Program();
        //on copie la variable dans un nouveau registre
        int newReg = getNewRegister();
        program.addInstruction(new UALi(UALi.Op.ADD, newReg, getVariableAddress(ctx.getText()), 0));

        return program;
    }

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
                break;
        }

        //si le rslt est faux, on met 1 dans le registre de destination
        program.addInstruction(new UALi(UALi.Op.ADD, destRegister, 0, 1));

        //Enfin, on inverse le résultat, donc 1 si c'est bon, 0 sinon : op : 1 - destRegister[=0 ou 1]
        program.addInstruction(new UAL(trueLabel.getLabel(),UAL.Op.SUB, destRegister, 1, destRegister));
        return program;
    }

    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        System.out.println("visitTab_initialization");

        
        Program program = new Program();
        int newReg = getNewRegister();
        // on récupère l'addresse du tableau 
        program.addInstruction(new UAL(UAL.Op.XOR, newReg, newReg, newReg));
        program.addInstruction(new UALi(UALi.Op.ADD, newReg, newReg, registerCounter - 2));
        for (int i = 0; i < ctx.expr().size(); i++) {
            if (i != 0 && i % 8 == 0)
            {
                // mettre stack pointer + 1
                program.addInstruction(new UALi(UALi.Op.ADD, 2,2,1));
                //on met l'adresse de la prochaine partie du tableau dans le registre
                program.addInstruction(new UALi(UALi.Op.ADD, newReg, newReg, 1));
                program.addInstruction(new Mem(Mem.Op.ST, 2, newReg));  
                // mettre stack pointer dans newReg
                program.addInstruction(new UALi(UALi.Op.ADD, newReg, 2, 0));
                program.addInstruction(new UALi(UALi.Op.ADD, 2, 2, 9));
            }
            // Visite chaque argument de la fonction
            Program argProgram = visit(ctx.expr(i));
            program.addInstructions(argProgram);
            //on les empiles, après avoir augmenté le stack pointer
            program.addInstruction(new UALi(UALi.Op.ADD, newReg, newReg,1));
            program.addInstruction(new Mem(Mem.Op.ST, registerCounter - 1, newReg));
        }
        return program;
    }
    
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

    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        System.out.println("visitBase_type");
        Program program = new Program();

        // Génère le code pour la déclaration du type de base
        program.addInstruction(new Label("BASE_TYPE"));

        //c'est soit un int soit un bool, ou un auto...


        return program;
    }

    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        System.out.println("visitTab_type");

        Program program = new Program();
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

        return program;
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        System.out.println("visitDeclaration");
        Program program = new Program();
        /* !!!prob : on ne peut avoir qu'une déclaration d'entier (ou de booléen des fois)
        */
        int destRegister = getNewRegister();
        // on déclare la variable dans la table des variables
        variableTable.put(ctx.VAR().getText(), destRegister);
        
        //on gère les tableaux (visit le type)
        program.addInstructions(visit(ctx.type()));


        if (ctx.expr() == null) return program;
        
        program.addInstructions(visit(ctx.expr()));
       
        // on déclare la variable : init à 0 puis on la modifie avec l'expression
        program.addInstruction(new UALi(UALi.Op.ADD, destRegister, registerCounter - 1, 0));
        return program;
    }
     
    @Override
    public Program visitPrint(grammarTCLParser.PrintContext ctx) {
        System.out.println("visitPrint");
        Program program = new Program();
        
        // Récupérer le nom de la variable à imprimer
        String varName = ctx.VAR().getText();
        
        // Récupérer le registre ou l'adresse associée à la variable
        int varAddress = variableTable.get(varName);

        program.addInstruction(new IO(IO.Op.PRINT, varAddress));

        return program;
    }
    
    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        System.out.println("visitAssignment");
        Program program = new Program();


        // Visite l'expression à droite de l'opérateur d'assignation
        Program expressionProgram = visit(ctx.expr(0)); // Utilise expr(0) pour obtenir l'expression
        // Ajoute les instructions générées pour l'expression à droite de l'opérateur d'assignation
        program.addInstructions(expressionProgram);
        // Ajoute une instruction pour stocker la valeur dans la variable
        int sr1 = registerCounter - 1;
        //sr1 est donc le registre contenant le résultat de l'expression
        int regvar = getVariableAddress(ctx.VAR().getText());

        program.addInstruction(new UALi(UALi.Op.ADD, regvar, sr1, 0));
        return program;
    }

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

    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        Program program = new Program();
        System.out.println("visitMain");
        program.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
        program.addInstruction(new UALi(UALi.Op.ADD, 1, 0, 1));
        program.addInstruction(new UAL(UAL.Op.XOR, 2, 2, 2));
        program.addInstruction(new Label("main"));
        // Générer le code pour le corps de la fonction principale
        Program coreFunctionProgram = visitCore_fct(ctx.core_fct());
        program.addInstructions(coreFunctionProgram);
        program.addInstruction(new Stop());

        // Générer le code pour chaque déclaration de fonction
        for (grammarTCLParser.Decl_fctContext declContext : ctx.decl_fct()) {
            Program declProgram = visitDecl_fct(declContext);
            program.addInstructions(declProgram);
        }
        return program;
    }
}
