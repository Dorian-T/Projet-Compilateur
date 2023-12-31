import java.util.Map;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Asm.Program;
import Type.Type;
import Type.UnknownType;

public class CodeGenerator  extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    private Map<UnknownType,Type> types;

    /**
     * Constructeur
     * @param types types de chaque variable du code source
     */
    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
    }

    @Override
    public Program visitNegation(grammarTCLParser.NegationContext ctx) {
        Program program = new Program();

        // Récupére le code généré pour l'expression à nier
        Program exprProgram = visit(ctx.expression());

        // Génére le code assembleur pour la négation
        int destRegister = getNewRegister();
        program.addAll(exprProgram);
        program.addCode("LOADI R" + destRegister + ", 1 ; Load 1 to a register for true");
        program.addCode("SUB R" + destRegister + ", R" + destRegister + ", R1 ; Subtract expression value from 1");
        program.addCode("PUSH R" + destRegister + " ; Push the negated value to the stack");

        return program;
    }


    @Override
    public Program visitComparison(grammarTCLParser.ComparisonContext ctx) {
        Program program = new Program();

        // Récupére le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expression(0));
        Program rightExprProgram = visit(ctx.expression(1));

        // Génére le code assembleur pour la comparaison
        int destRegister = getNewRegister();
        program.addAll(leftExprProgram);
        program.addAll(rightExprProgram);

        // Compare les deux valeurs
        String operator = ctx.getChild(1).getText();
        switch (operator) {
            case "==":
                program.addCode("POP R2 ; Pop the right operand from the stack");
                program.addCode("POP R1 ; Pop the left operand from the stack");
                program.addCode("SUB R" + destRegister + ", R1, R2 ; Subtract left operand from right operand");
                program.addCode("JZ R" + destRegister + ", TrueLabel ; Jump to TrueLabel if the result is zero");
                break;
            case "!=":
                //...
                break;
            case ">":
                // ...
                break;
            default:
                throw new UnsupportedOperationException("Unsupported comparison operator: " + operator);
        }

        //  cas False
        program.addCode("LOADI R" + destRegister + ", 0 ; Load 0 to indicate False");
        program.addCode("PUSH R" + destRegister + " ; Push the result to the stack");
        program.addCode("JMP EndLabel ; Jump to EndLabel");

        //  cas True
        program.addCode("TrueLabel: LOADI R" + destRegister + ", 1 ; Load 1 to indicate True");
        program.addCode("PUSH R" + destRegister + " ; Push the result to the stack");

        program.addCode("EndLabel: ; End of comparison");

        return program;
    }


    @Override
    public Program visitOr(grammarTCLParser.OrContext ctx) {
        Program program = new Program();

        // Récupére le code généré pour les deux expressions à comparer
        Program leftExprProgram = visit(ctx.expression(0));
        Program rightExprProgram = visit(ctx.expression(1));

        // Génére le code assembleur pour l'opération logique "||"
        int destRegister = getNewRegister();
        program.addAll(leftExprProgram);

        // Jump to TrueLabel if left operand is true
        program.addCode("POP R" + destRegister + " ; Pop the left operand from the stack");
        program.addCode("JNZ R" + destRegister + ", TrueLabel ; Jump to TrueLabel if the left operand is true");

        // If the left operand is false, evaluate the right operand
        program.addAll(rightExprProgram);
        program.addCode("TrueLabel: ; TrueLabel, the left operand is true");
        program.addCode("POP R" + destRegister + " ; Pop the right operand from the stack");

        // Push the result of the OR operation to the stack
        program.addCode("OR R" + destRegister + ", R" + destRegister + ", R1 ; Perform OR operation");
        program.addCode("PUSH R" + destRegister + " ; Push the result to the stack");

        return program;
    }


    @Override
    public Program visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Program program = new Program();

        // Récupére le code généré pour l'expression à nier
        Program exprProgram = visit(ctx.expression());

        // Génére le code assembleur pour l'opération logique "!"
        int destRegister = getNewRegister();
        program.addAll(exprProgram);

        // Jump to TrueLabel if the operand is false
        program.addCode("POP R" + destRegister + " ; Pop the operand from the stack");
        program.addCode("JZ R" + destRegister + ", TrueLabel ; Jump to TrueLabel if the operand is false");

        // If the operand is true, push false to the stack
        program.addCode("FalseLabel: LOADI R" + destRegister + ", 0 ; Load 0 to indicate False");
        program.addCode("PUSH R" + destRegister + " ; Push the result to the stack");
        program.addCode("JMP EndLabel ; Jump to EndLabel");

        // If the operand is false, push true to the stack
        program.addCode("TrueLabel: LOADI R" + destRegister + ", 1 ; Load 1 to indicate True");
        program.addCode("PUSH R" + destRegister + " ; Push the result to the stack");

        program.addCode("EndLabel: ; End of negation");

        return program;
    }


    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        Program program = new Program();

        // Récupére la valeur entière à partir du contexte
        int integerValue = Integer.parseInt(ctx.INTEGER().getText());

        // Génére le code assembleur pour charger la constante entière dans un registre
        int destRegister = getNewRegister();
        program.addCode("LOADI R" + destRegister + ", " + integerValue + " ; Load integer value to a register");
        program.addCode("PUSH R" + destRegister + " ; Push the integer value to the stack");

        return program;
    }


    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        Program program = new Program();

        // Récupérer le type du tableau et l'indice
        Type tabType = types.get(ctx.ID().getText());
        Program indexProgram = visit(ctx.expression());

        // Générer le code assembleur pour l'accès au tableau
        program.addAll(indexProgram);
        program.addCode("POP R2 ; Pop the index value from the stack");
        program.addCode("LOAD R1, (R1) ; Load the base address of the array");
        program.addCode("ADD R1, R1, R2 ; Add the index to the base address");
        program.addCode("PUSH R1 ; Push the result (address of the array element) to the stack");

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
        program.addCode("LOADI R" + arrayDestRegister + ", " + arraySize + " ; Load array size to a register");

        // logique pour l'allocation de mémoire à faire 
        // ...

        int currentElementRegister = arrayDestRegister + 1;

        // Générer le code pour chaque élément du tableau
        for (int i = 0; i < arraySize; i++) {
            Program exprProgram = visit(ctx.expression(i));
            program.addAll(exprProgram);
            program.addCode("STORE R" + currentElementRegister + ", (R" + arrayDestRegister + ") ; Store value in the array");
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
