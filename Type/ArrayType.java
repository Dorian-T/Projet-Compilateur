package Type;
import java.util.HashMap;
import java.util.Map;

public class ArrayType extends Type{
    private Type tabType;
    
    /**
     * Constructeur
     * @param t type des éléments du tableau
     */
    public ArrayType(Type t) {
        this.tabType = t;
    }

    /**
     * Getter du type des éléments du tableau
     * @return type des éléments du tableau
     */
    public Type getTabType() {
       return tabType;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        Map<UnknownType,Type> resultat = new HashMap<UnknownType,Type>();
        if(this.equals(t) || t == null){
            return null;
        }else if(t instanceof UnknownType){
            resultat.put((UnknownType) t, this);
            return resultat;
        }else if(t instanceof ArrayType){
            resultat = this.getTabType().unify(((ArrayType) t).getTabType()); //TODO : vérifier que ça marche (ca marche pas)
            
            throw new Error("Uniplemented method unify for ArrayType and ArrayType");
            // if (resultat.isEmpty()) {
            //     return null;
            // }else if(resultat.containsKey(this.getTabType())){
                
                
            // }
            
            
        }else{
            throw new Error("Unification error");
        }
    }

    @Override
    public boolean equals(Object t) {
        return (t instanceof ArrayType) && (((ArrayType)t).getTabType().equals(this.tabType));
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'substitute'");
    }

    @Override
    public boolean contains(UnknownType v) {
        return this.tabType.contains(v);
    }

    /**
     * Returns a string representation of the ArrayType.
     *
     * @return a string representation of the ArrayType
     */
    @Override
    public String toString() {
        return "array(" + tabType.toString() + ")";
    }
}
