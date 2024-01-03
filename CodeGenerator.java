import java.util.Map;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Asm.Program;
import Asm.Ret;
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

public class CodeGenerator  extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    private Map<UnknownType,Type> types;
    private int registerCounter;
    private Map<String, Integer> variableTable;
    private int labelCounter;
    private int conditionRegister;

    /**
     * Constructeur
     * @param types types de chaque variable du code source
     */
    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
        this.registerCounter = 0;
        this.variableTable = new java.util.HashMap<String, Integer>();
        this.labelCounter = 0;
        this.conditionRegister = 0;
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
    
    @Override
    public Program visitNegation(grammarTCLParser.NegationContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour l'expression à nier
        Program exprProgram = visit(ctx.expr());

        // Génère le code assembleur pour la négation
        int destRegister = getNewRegister();
        
        // Ajoute toutes les instructions de l'expression à nier
        program.addInstructions(exprProgram);

        // Ajoute une instruction pour charger 1 dans le registre de destination
        program.addInstruction(new UALi("LOADI", UALi.Op.ADD, destRegister, 0, 1));

        // Ajoute une instruction pour soustraire l'expression du registre de destination
        program.addInstruction(new UALi("SUB", UALi.Op.SUB, destRegister, destRegister, 1));

        // Ajoute une instruction pour pousser la valeur négative sur la pile
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        return program;
    }

    @Override
    public Program visitComparison(grammarTCLParser.ComparisonContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expr(0));
        Program rightExprProgram = visit(ctx.expr(1));

        // Génère le code assembleur pour la comparaison
        int destRegister = getNewRegister();
        program.addInstructions(leftExprProgram);
        program.addInstructions(rightExprProgram);

        // Compare les deux valeurs
        String operator = ctx.getChild(1).getText();
        switch (operator) {
            case "==":
                program.addInstruction(new CondJump("TrueLabel", CondJump.Op.JEQU, destRegister, 0, "TrueLabel"));
                break;
            case "!=":
                program.addInstruction(new CondJump("FalseLabel", CondJump.Op.JEQU, destRegister, 0, "FalseLabel"));
                
                // Cas True
                program.addInstruction(new UALi("TrueLabel", UALi.Op.ADD, destRegister, 0, 1));
                program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel"));
                
                // Cas False
                program.addInstruction(new UALi("FalseLabel", UALi.Op.ADD, destRegister, 0, 0));
                break;
            case ">":
                program.addInstruction(new CondJump("TrueLabel", CondJump.Op.JSUP, destRegister, 0, "TrueLabel"));
                
                // Cas False
                program.addInstruction(new UALi("FalseLabel", UALi.Op.ADD, destRegister, 0, 0));
                program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel"));
                
                // Cas True
                program.addInstruction(new JumpCall("TrueLabel", JumpCall.Op.JMP, "TrueLabel"));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported comparison operator: " + operator);
        }

        // Cas False
        program.addInstruction(new UALi("EndLabel", UALi.Op.ADD, destRegister, 0, 0));

        // Cas True
        program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel"));

        program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel"));

        return program;
    }

    @Override
    public Program visitOr(grammarTCLParser.OrContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expr(0));
        Program rightExprProgram = visit(ctx.expr(1));

        // Génère le code assembleur pour l'opération logique "||"
        int destRegister = getNewRegister();
        program.addInstructions(leftExprProgram);

        // Sauter à l'étiquette TrueLabel si l'opérande de gauche est vrai
        program.addInstruction(new IO("POP", IO.Op.OUT, destRegister));
        program.addInstruction(new CondJump("TrueLabel", CondJump.Op.JNEQ, destRegister, 0, "TrueLabel"));

        // Si l'opérande de gauche est faux, évaluez l'opérande de droite
        program.addInstructions(rightExprProgram);
        program.addInstruction(new JumpCall("TrueLabel", JumpCall.Op.JMP, "TrueLabel"));

        // TrueLabel, l'opérande de gauche est vrai
        program.addInstruction(new IO("POP", IO.Op.OUT, destRegister));

        // Effectuer l'opération logique OR
        program.addInstruction(new UAL("OR", UAL.Op.OR, destRegister, destRegister, 1));

        // Poussez le résultat de l'opération OR sur la pile
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        return program;
    }
 
    @Override
    public Program visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour l'expression à nier
        Program exprProgram = visit(ctx.expr());

        // Génère le code assembleur pour l'opération logique "!"
        int destRegister = getNewRegister();
        program.addInstructions(exprProgram);

        // Sauter à l'étiquette TrueLabel si l'opérande est faux
        program.addInstruction(new IO("POP", IO.Op.OUT, destRegister));
        program.addInstruction(new CondJump("TrueLabel", CondJump.Op.JZ, destRegister, 0, "TrueLabel"));

        // Si l'opérande est vrai, pousser faux sur la pile
        program.addInstruction(new JumpCall("FalseLabel", JumpCall.Op.JMP, "FalseLabel"));
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));
        program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel"));

        // Si l'opérande est faux, pousser vrai sur la pile
        program.addInstruction(new JumpCall("TrueLabel", JumpCall.Op.JMP, "TrueLabel"));
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        // Fin de la négation
        program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel"));

        return program;
    }

    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        Program program = new Program();

        // Récupère la valeur entière à partir du contexte
        int integerValue = Integer.parseInt(ctx.INT().getText());

        // Génère le code assembleur pour charger la constante entière dans un registre
        int destRegister = getNewRegister();
        program.addInstruction(new UALi("LOADI", UALi.Op.LOADI, destRegister, 0, integerValue));
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        return program;
    }

    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        Program program = new Program();

        // Récupérer le type du tableau et l'indice
        Type tabType = types.get(ctx.ID().getText());

        // Générer le code assembleur pour chaque expression dans la liste
        for (grammarTCLParser.ExprContext exprCtx : ctx.expr()) {
            Program indexProgram = visit(exprCtx);
            program.addInstructions(indexProgram);
            program.addInstruction(new UAL("POP", UAL.Op.POP, 2, 0, 0)); // Récupérer la valeur de l'indice de la pile
            program.addInstruction(new Mem("LOAD", Mem.Op.LD, 1, 1)); // Charger l'adresse de base du tableau
            program.addInstruction(new UAL("ADD", UAL.Op.ADD, 1, 1, 2)); // Ajouter l'indice à l'adresse de base
            program.addInstruction(new IO("PUSH", IO.Op.OUT, 1)); // Pousser le résultat (adresse de l'élément du tableau) sur la pile
        }

        return program;
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
        Program program = new Program();
    
        // Récupère le nom de la fonction à appeler depuis le contexte
        String functionName = ctx.VAR().getText(); // Utilise VAR() au lieu de ID()
    
        // Sauvegarde l'état actuel
        program.addInstruction(new UAL("PUSH", UAL.Op.PUSH, 0, 0, 0));
        program.addInstruction(new UAL("PUSH", UAL.Op.PUSH, 1, 1, 0));
        program.addInstruction(new UAL("PUSH", UAL.Op.PUSH, 2, 2, 0));
        program.addInstruction(new UAL("PUSH", UAL.Op.PUSH, 3, 3, 0));
    
        // Génère le code pour chaque argument de la fonction
        for (grammarTCLParser.ExprContext exprContext : ctx.expr()) {
            Program argProgram = visit(exprContext);
            program.addInstructions(argProgram);
        }
    
        // Génère le code d'appel de la fonction
        program.addInstruction(new JumpCall(functionName, JumpCall.Op.JMP, functionName));
    
        // Restaure l'état après l'appel de fonction
        program.addInstruction(new UAL("POP", UAL.Op.POP, 3, 3, 0));
        program.addInstruction(new UAL("POP", UAL.Op.POP, 2, 2, 0));
        program.addInstruction(new UAL("POP", UAL.Op.POP, 1, 1, 0));
        program.addInstruction(new UAL("POP", UAL.Op.POP, 0, 0, 0));
    
        return program;
    }
    
    @Override
    public Program visitBoolean(grammarTCLParser.BooleanContext ctx) {
        Program program = new Program();

        // Récupère la valeur booléenne depuis le contexte
        boolean booleanValue = Boolean.parseBoolean(ctx.BOOL().getText());

        // Génère le code pour charger la valeur booléenne dans un registre
        int destRegister = getNewRegister();
        program.addInstruction(new UALi("LOADI", UALi.Op.LOADI, destRegister, 0, booleanValue ? 1 : 0));
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister)); // Pousse la valeur sur la pile

        return program;
    }

    @Override
    public Program visitAnd(grammarTCLParser.AndContext ctx) {
        Program program = new Program();
    
        // Récupère le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expr(0));
        Program rightExprProgram = visit(ctx.expr(1));
    
        // Génère le code assembleur pour l'opération logique "&&"
        int destRegister = getNewRegister();
        program.addInstructions(leftExprProgram);
        program.addInstruction(new IO("POP", IO.Op.OUT, destRegister)); // Pop la valeur de l'opérande de gauche
        program.addInstruction(new CondJump("FalseLabel", CondJump.Op.JZ, destRegister, 0, "FalseLabel")); // Sauter à FalseLabel si l'opérande de gauche est faux
    
        // Si l'opérande de gauche est vrai, évaluer l'opérande de droite
        program.addInstructions(rightExprProgram);
        program.addInstruction(new IO("POP", IO.Op.OUT, destRegister)); // Pop la valeur de l'opérande de droite
        program.addInstruction(new CondJump("FalseLabel", CondJump.Op.JZ, destRegister, 0, "FalseLabel")); // Sauter à FalseLabel si l'opérande de droite est faux
    
        // Si les deux opérandes sont vrais, pousser vrai sur la pile
        program.addInstruction(new JumpCall("TrueLabel", JumpCall.Op.JMP, "TrueLabel"));
    
        // FalseLabel: pousser faux sur la pile
        program.addInstruction(new UALi("LOADI", UALi.Op.LOADI, destRegister, 0, 0));
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));
    
        // TrueLabel: étiquette de fin
        program.addInstruction(new Label("TrueLabel: ; TrueLabel, both operands are true"));
    
        return program;
    }
    
    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        Program program = new Program();

        // Récupérer le nom de la variable depuis le contexte
        String variableName = ctx.VAR().getText();

        // Générer le code pour charger la valeur de la variable sur la pile
        int destRegister = getNewRegister();
        int variableAddress = getVariableAddress(variableName); // Assurez-vous d'implémenter cette méthode
        program.addInstruction(new Mem("LOAD", Mem.Op.LD, destRegister, variableAddress));

        // Pousse la valeur de la variable sur la pile
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        return program;
    }

    @Override
    public Program visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à multiplier
        Program leftExprProgram = visit(ctx.expr(0));
        Program rightExprProgram = visit(ctx.expr(1));

        // Génère le code assembleur pour l'opération de multiplication
        int destRegister = getNewRegister();
        program.addInstructions(leftExprProgram);
        program.addInstructions(rightExprProgram);
        program.addInstruction(new UAL("MUL", UAL.Op.MUL, destRegister, 1, 2)); // Multiplication

        // Pousse le résultat sur la pile
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        return program;
    }

    @Override
    public Program visitEquality(grammarTCLParser.EqualityContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expr(0));
        Program rightExprProgram = visit(ctx.expr(1));

        // Génère le code assembleur pour l'opération d'égalité
        int destRegister = getNewRegister();
        program.addInstructions(leftExprProgram);
        program.addInstructions(rightExprProgram);
        
        // Compare les deux valeurs
        program.addInstruction(new UAL("SUB", UAL.Op.SUB, destRegister, 1, 2)); // Soustraction
        program.addInstruction(new CondJump("TrueLabel", CondJump.Op.JZ, destRegister, 0, "TrueLabel")); // Jump to TrueLabel if the result is zero

        // Si les deux valeurs sont différentes, pousser faux sur la pile
        program.addInstruction(new UALi("LOADI", UALi.Op.LOADI, destRegister, 0, 0)); // Load 0 to indicate False
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister)); // Push the result to the stack
        program.addInstruction(new JumpCall("EndLabel", JumpCall.Op.JMP, "EndLabel")); // Jump to EndLabel

        // TrueLabel: si les deux valeurs sont égales, pousser vrai sur la pile
        program.addInstruction(new UALi("LOADI", UALi.Op.LOADI, destRegister, 1, 1)); // Load 1 to indicate True
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister)); // Push the result to the stack

        // EndLabel: étiquette de fin
        program.addInstruction(new Label("EndLabel: ; End of equality"));

        return program;
    }

    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        Program program = new Program();
    
        // Récupérer le type du tableau
       // Type tabType = types.get(ctx.getChild(0).getText()); // Assurez-vous que le nom du type est au bon endroit dans votre grammaire
    
        // Générer le code pour l'allocation de mémoire pour le tableau
        int arraySize = ctx.expr().size();
        int arrayDestRegister = getNewRegister();
        program.addInstruction(new UALi("LOADI", UALi.Op.LOADI, arrayDestRegister, 0, arraySize));
    
        // Logique pour l'allocation de mémoire à faire 
        // ...
    
        int currentElementRegister = arrayDestRegister + 1;
    
        // Générer le code pour chaque élément du tableau
        for (int i = 0; i < arraySize; i++) {
            // Visite chaque expression dans la liste
            Program exprProgram = visit(ctx.expr(i));
            // Ajoute les instructions générées pour l'expression courante
            program.addInstructions(exprProgram);
            // Ajoute une instruction pour stocker la valeur dans le tableau
            program.addInstruction(new Mem("STORE", Mem.Op.ST, currentElementRegister, arrayDestRegister));
            currentElementRegister++;
        }
    
        return program;
    }
    
    @Override
    public Program visitAddition(grammarTCLParser.AdditionContext ctx) {
        Program program = new Program();

        // Récupère le code généré pour les deux expressions à additionner
        Program leftExprProgram = visit(ctx.expr(0));
        Program rightExprProgram = visit(ctx.expr(1));

        // Ajoute les instructions générées pour les expressions à additionner
        program.addInstructions(leftExprProgram);
        program.addInstructions(rightExprProgram);

        // Génère le code pour l'opération d'addition
        int destRegister = getNewRegister();
        program.addInstruction(new UAL("ADD", UAL.Op.ADD, destRegister, 0, 1));

        // Pousse le résultat sur la pile
        program.addInstruction(new IO("PUSH", IO.Op.OUT, destRegister));

        return program;
    }

    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        Program program = new Program();

        // Récupère le nom du type de base depuis le contexte
        //String typeName = ctx.BASE_TYPE().getText();

        // Génère le code pour la déclaration du type de base
        program.addInstruction(new Mem("", Mem.Op.ST, getNewRegister(), 0)); // Déclaration du type de base

        return program;
    }

    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        Program program = new Program();

        // Récupère le nom du type de base depuis le sous-contexte de type
        String baseTypeName = ctx.type().BASE_TYPE().getText();

        // Récupère la taille du tableau depuis le sous-contexte INT
        int arraySize = Integer.parseInt(ctx.INT().getText());

        // Génère le code pour la déclaration du tableau
        program.addInstruction(new Label(baseTypeName)); // Crée une étiquette pour le type de base
        program.addInstruction(new Mem("ALLOC", Mem.Op.LD, getNewRegister(), arraySize)); // Alloue de la mémoire pour le tableau

        return program;
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        Program program = new Program();
    
        // Récupère le nom de la variable depuis le contexte
        String variableName = ctx.VAR().getText();
        
        // Récupère le type depuis le contexte
        String typeName = ctx.type().getText();
    
        // Génère le code pour la déclaration de la variable
        program.addInstruction(new UALi("DECL", UALi.Op.LOADI, variableName.hashCode(), 0, Integer.parseInt(typeName))); 
    
        return program;
    }
     
    @Override
    public Program visitPrint(grammarTCLParser.PrintContext ctx) {
        Program program = new Program();
    
        // Récupère le texte associé au contexte PrintContext
        //String expressionText = ctx.getText();
    
        // Génère le code pour l'instruction d'impression
        int destRegister = getNewRegister();
        program.addInstruction(new IO("OUT", IO.Op.OUT, destRegister));
    
        return program;
    }
    
    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        Program program = new Program();

        //String variableName = ctx.VAR().getText(); // Récupère le nom de la variable depuis le contexte
        // Visite l'expression à droite de l'opérateur d'assignation
        Program expressionProgram = visit(ctx.expr(0)); // Utilise expr(0) pour obtenir la première expression

        // Génère le code pour l'assignation
        for (Instruction instruction : expressionProgram.getInstructions()) {
            program.addInstruction(instruction); // Ajoute chaque instruction de l'expression
        }
        program.addInstruction(new Mem("", Mem.Op.ST, getNewRegister(), 0)); // Assignation à la variable

        return program;
    }

    @Override
    public Program visitBlock(grammarTCLParser.BlockContext ctx) {
        Program program = new Program();

        // Visite chaque instruction dans le bloc
        for (grammarTCLParser.InstrContext instrContext : ctx.instr()) {
            Program instructionProgram = visit(instrContext);
            program.addInstructions(instructionProgram);
        }

        return program;
    }

    @Override
    public Program visitIf(grammarTCLParser.IfContext ctx) {
        Program program = new Program();

        // Génère le code pour l'expression conditionnelle
        Program conditionProgram = visit(ctx.expr());
        program.addInstructions(conditionProgram);

        // Crée une instruction de saut conditionnel
        String trueLabel = generateNewLabel();
        program.addInstruction(new CondJump(trueLabel, CondJump.Op.JNEQ, conditionRegister, 0, ""));

        // Génère le code pour le bloc if
        Program trueBlockProgram = visit(ctx.instr(0));
        program.addInstructions(trueBlockProgram);

        // Ajoute une étiquette pour le saut conditionnel
        program.addInstruction(new Label(trueLabel));

        // Si un bloc else existe, génère le code pour le bloc else
        if (ctx.instr().size() > 1) {
            Program falseBlockProgram = visit(ctx.instr(1)); 
            program.addInstructions(falseBlockProgram);
        }

        return program;
    }

    @Override
    public Program visitWhile(grammarTCLParser.WhileContext ctx) {
        Program program = new Program();
    
        // Étiquettes pour le saut conditionnel et la fin de la boucle
        String conditionLabel = generateNewLabel();
        String endLabel = generateNewLabel();
    
        // Ajoute une étiquette pour la condition initiale
        program.addInstruction(new Label(conditionLabel));
    
        // Génère le code pour l'expression conditionnelle
        Program conditionProgram = visit(ctx.expr());
        for (Instruction instruction : conditionProgram.getInstructions()) {
            program.addInstruction(instruction);
        }
    
        // Crée une instruction de saut conditionnel
        program.addInstruction(new CondJump(endLabel, CondJump.Op.JNEQ, conditionRegister, 0, ""));
    
        // Génère le code pour le bloc de la boucle
        Program loopBlockProgram = visit(ctx.instr());
        for (Instruction instruction : loopBlockProgram.getInstructions()) {
            program.addInstruction(instruction);
        }
    
        // Ajoute une étiquette pour le saut conditionnel de retour
        program.addInstruction(new JumpCall(conditionLabel, JumpCall.Op.JMP, ""));
    
        // Ajoute une étiquette pour la fin de la boucle
        program.addInstruction(new Label(endLabel));
    
        return program;
    }
    
    @Override
    public Program visitFor(grammarTCLParser.ForContext ctx) {
        Program program = new Program();

        // Générer le code pour l'initialisation
        Program initProgram = visit(ctx.instr(0)); 
        program.addInstructions(initProgram);

        // Étiquette de condition de boucle
        String loopConditionLabel = generateNewLabel();
        program.addInstruction(new Label(loopConditionLabel));

        // Générer le code pour la condition de la boucle
        Program conditionProgram = visit(ctx.expr());
        program.addInstructions(conditionProgram);

        // Supposons qu'il y ait une instruction de saut basée sur la condition
        program.addInstruction(new CondJump("JMP", CondJump.Op.JZ, getNewRegister(), 0, "EXIT_LOOP"));

        // Générer le code pour le corps de la boucle
        Program bodyProgram = visit(ctx.instr(1)); 
        program.addInstructions(bodyProgram);

        // Générer le code pour l'incrémentation
        Program incrementProgram = visit(ctx.instr(2));
        program.addInstructions(incrementProgram);

        // Sauter de nouveau à la condition de la boucle
        program.addInstruction(new JumpCall("JMP", JumpCall.Op.JMP, loopConditionLabel));

        // Étiquette de sortie
        program.addInstruction(new Label("EXIT_LOOP"));

        return program;
    }

    @Override
    public Program visitReturn(grammarTCLParser.ReturnContext ctx) {

        // Créer un programme pour la valeur de retour
        Program returnProgram = visit(ctx.expr());

        // Ajouter les instructions générées pour la valeur de retour au programme global
        Program program = new Program();
        program.addInstructions(returnProgram);

        // Ajouter l'instruction de retour
        program.addInstruction(new Ret());

        return program;
    }

    @Override
    public Program visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        Program program = new Program();

        // Générer le code pour chaque instruction dans la fonction de base
        for (grammarTCLParser.InstrContext instrContext : ctx.instr()) {
            Program instrProgram = visit(instrContext);
            program.addInstructions(instrProgram);
        }

        return program;
    }

    @Override
    public Program visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        Program program = new Program();
    
        // Générer le code pour la déclaration de la fonction
        String functionName = ctx.VAR(0).getText(); // Récupérer le nom de la fonction à partir de VAR(0)
        program.addInstruction(new Label(functionName));
    
        // Générer le code pour les paramètres de la fonction, si nécessaire
        // ...
    
        // Générer le code pour le corps de la fonction
        Program coreFunctionProgram = visit(ctx.core_fct());
        program.addInstructions(coreFunctionProgram);
    
        return program;
    }    

    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        Program program = new Program();

        // Générer le code pour chaque déclaration de fonction
        for (grammarTCLParser.Decl_fctContext declContext : ctx.decl_fct()) {
            Program declProgram = visitDecl_fct(declContext);
            program.addInstructions(declProgram);
        }

        // Générer le code pour le corps de la fonction principale
        Program coreFunctionProgram = visitCore_fct(ctx.core_fct());
        program.addInstructions(coreFunctionProgram);

        return program;
    }
}
