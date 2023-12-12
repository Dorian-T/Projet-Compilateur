package Type;
import java.util.Map;


public abstract class Type {
    public enum Base { INT, BOOL };

    /** 
     * Unification
     * @param t type à unifier
     * @return la liste des substitutions à effectuer (null si pas unifiable)
     */
    public abstract Map<UnknownType,Type> unify(Type t); 

    /** 
     * Test d'égalité
     * @param t type 
     * @return boolean 
     */
    public abstract boolean equals(Type t);

    /** 
     * Substitution
     * @param v type variable à substituer
     * @param t type par lequel remplacer v
     * @return Type obtenu en remplaçant v par t
     */
    public abstract Type substitute(UnknownType v, Type t);
    
    /**
     * Applique plusieurs substitutions
     * @param h liste de substitutions
     * @return Type obtenu en appliquant toutes les substitutions de h
     */
    public Type substituteAll(Map<UnknownType,Type> h){
        Type result = this;
        for (UnknownType key : h.keySet()) 
            result = result.substitute(key, h.get(key));
        return result;
    }
    
    /** 
     * Test si le type dépend du type variable v
     * @param v type variable
     * @return boolean 
     */
    public abstract boolean contains(UnknownType v);
}
