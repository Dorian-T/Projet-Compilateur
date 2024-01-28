package Type;
import java.util.ArrayList;
import java.util.Map;

public class FunctionType extends Type {
    private Type returnType;
    private ArrayList<Type> argsTypes;
    
    /**
     * Constructeur
     * @param returnType type de retour
     * @param argsTypes liste des types des arguments
     */
    public FunctionType(Type returnType, ArrayList<Type> argsTypes) {
        this.returnType = returnType;
        this.argsTypes = argsTypes;
    }

    /**
     * Getter du type de retour
     * @return type de retour
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * Getter du type du i-eme argument
     * @param i entier
     * @return type du i-eme argument
     */
    public Type getArgsType(int i) {
        return argsTypes.get(i);
    }

    /**
     * Getter du nombre d'arguments
     * @return nombre d'arguments
     */
    public int getNbArgs() {
        return argsTypes.size();
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        throw new UnsupportedOperationException("unify whith FunctionType ????? ok but NO !");
    }

    @Override
    public boolean equals(Object t) {
        if(!(t instanceof FunctionType))
            return false;
        if(!(((FunctionType)t).getReturnType().equals(this.returnType)))
            return false;
        if(((FunctionType)t).getNbArgs() != this.getNbArgs())
            return false;
        for(int i = 0; i < this.getNbArgs(); i++)
            if(!(((FunctionType)t).getArgsType(i).equals(this.getArgsType(i))))
                return false;
        return true;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        return this;
    }

    @Override
    public boolean contains(UnknownType v) {
        return this.returnType.contains(v);
    }

    /**
     * Returns a string representation of the FunctionType.
     *
     * @return a string representation of the FunctionType
     */
    @Override
    public String toString() {
        String str = "";
        str += returnType.toString() + "( ";
        for(int i = 0; i < argsTypes.size(); i++)
            str += (argsTypes.get(i).toString() + " ");
        return str.toString() + ")";
    }
}
