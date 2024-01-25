package Type;
import java.util.HashMap;
import java.util.Map;

public class Primitive_Type extends Type {
    
    private Type.Base type; 
    
    /**
     * Constructeur
     * @param type type de base
     */
    public Primitive_Type(Type.Base type) {
        this.type = type;
    }

    /**
     * Getter du type
     * @return type
     */
    public Type.Base getType() {
        return type;
    }

    @Override
    public boolean equals(Object t) {        
        return (t instanceof Primitive_Type) && 
            (((Primitive_Type)t).getType().toString().equals(this.type.toString()));
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        Map<UnknownType,Type> resultat = new HashMap<UnknownType,Type>();
        if(this.equals(t) || t == null){
            return null;
        }else if(t instanceof UnknownType){
            resultat.put((UnknownType) t, this);
            return resultat;
        }else{
            throw new RuntimeException("Unification error : Primitive_Type.unify between " + this + " and " + t);
        }
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        return this;
    }

    @Override
    public boolean contains(UnknownType v) {
        return false;
    }

    /**
     * Returns a string representation of the Primitive_Type.
     *
     * @return a string representation of the Primitive_Type
     */
    @Override
    public String toString() {
        if (type.equals(Type.Base.INT))
            return "INT";
        else
            return "BOOL";
    }
}
