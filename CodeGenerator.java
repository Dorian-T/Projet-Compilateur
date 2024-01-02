import java.util.Map;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Asm.Program;
import Asm.UAL;
import Asm.UALi;
import Asm.CondJump;
import Asm.IO;
import Asm.Instruction;
import Asm.JumpCall;
import Asm.Mem;
import Type.Type;
import Type.UnknownType;

public class CodeGenerator  extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    private Map<UnknownType,Type> types;
    private int registerCounter = 0;

    /**
     * Constructeur
     * @param types types de chaque variable du code source
     */
    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
    }

    /**
     * Obtient un nouveau numéro de registre disponible.
     * @return Un nouveau numéro de registre
     */
    private int getNewRegister() {
        // Incrémente le compteur et renvoie le nouveau numéro de registre
        return registerCounter++;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBrackets'");
    }

    @Override
    public Program visitCall(grammarTCLParser.CallContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
    }

    @Override
    public Program visitBoolean(grammarTCLParser.BooleanContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBoolean'");
    }

    @Override
    public Program visitAnd(grammarTCLParser.AndContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAnd'");
    }

    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVariable'");
    }

    @Override
    public Program visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMultiplication'");
    }

    @Override
    public Program visitEquality(grammarTCLParser.EqualityContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEquality'");
    }

    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        Program program = new Program();

        // Récupérer le type du tableau
        Type tabType = types.get(ctx.ID().getText());

        // Générer le code pour l'allocation de mémoire pour le tableau
        int arraySize = ctx.INT().size();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAddition'");
    }

    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBase_type'");
    }

    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_type'");
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDeclaration'");
    }

    @Override
    public Program visitPrint(grammarTCLParser.PrintContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPrint'");
    }

    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignment'");
    }

    @Override
    public Program visitBlock(grammarTCLParser.BlockContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlock'");
    }

    @Override
    public Program visitIf(grammarTCLParser.IfContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIf'");
    }

    @Override
    public Program visitWhile(grammarTCLParser.WhileContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhile'");
    }

    @Override
    public Program visitFor(grammarTCLParser.ForContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFor'");
    }

    @Override
    public Program visitReturn(grammarTCLParser.ReturnContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturn'");
    }

    @Override
    public Program visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCore_fct'");
    }

    @Override
    public Program visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDecl_fct'");
    }

    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMain'");
    }

        
}
