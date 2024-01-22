package Type;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class UnknownType extends Type {
    private String varName;

    private int varIndex;
    private static int newVariableCounter = 0;

    /**
     * Constructeur sans nom
     */
    public UnknownType() {
        this.varIndex = newVariableCounter++;
        this.varName = "#";
    }

    /**
     * Constructeur à partir d'un nom de variable et un numéro
     * 
     * @param s nom de variable
     * @param n numéro de la variable
     */
    public UnknownType(String s, int n) {
        this.varName = s;
        this.varIndex = n;
    }

    /**
     * Constructeur à partir d'un ParseTree (standardisation du nom de variable)
     * 
     * @param ctx ParseTree
     */
    public UnknownType(ParseTree ctx) {
        this.varName = ctx.getText();
        if (ctx instanceof TerminalNode) {
            this.varIndex = ((TerminalNode) ctx).getSymbol().getStartIndex();
        } else {
            if (ctx instanceof ParserRuleContext) {
                this.varIndex = ((ParserRuleContext) ctx).getStart().getStartIndex();
            } else {
                throw new Error("Illegal UnknownType construction");
            }
        }
    }

    /**
     * Getter du nom de variable de type
     * 
     * @return variable de type
     */
    public String getVarName() {
        return varName;
    }

    /**
     * Getter du numéro de variable de type
     * 
     * @return numéro de variable de type
     */
    public int getVarIndex() {
        return varIndex;
    }

    /**
     * Setter du numéro de variable de type
     * 
     * @param n numéro de variable de type
     */
    public void setVarIndex(int n) {
        this.varIndex = n;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        Map<UnknownType, Type> resultat = new HashMap<UnknownType, Type>();

        if (t instanceof UnknownType) {
            if (this.getVarName().startsWith("#"))
                resultat.put(this, t);
            else
                resultat.put((UnknownType) t, this);
            
            return resultat;
        }
        
        return t.unify(this);
    }

    @Override
    public boolean equals(Object t) {
        return (t instanceof UnknownType) && (((UnknownType) t).getVarName().equals(this.varName));
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        if (this.equals(v)) {
            return t;
        } else {
            return this;
        }
    }

    @Override
    public boolean contains(UnknownType v) {
        return this.equals(v);
    }

    /**
     * Returns a string representation of the UnknownType.
     *
     * @return a string representation of the UnknownType
     */
    @Override
    public String toString() {
        return varName;
    }
}