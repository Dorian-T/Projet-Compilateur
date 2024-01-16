package Type;
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
    public Map<UnknownType, Type> unify(Type t) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unify'");
    }

    @Override
    public boolean equals(Object t) {
        return (t instanceof Primitive_Type) && (((Primitive_Type)t).getType() == this.type);
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'substitute'");
    }

    @Override
    public boolean contains(UnknownType v) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }

    /**
     * Returns a string representation of the Primitive_Type.
     *
     * @return a string representation of the Primitive_Type
     */
    @Override
    public String toString() {
        if (type == Type.Base.INT)
            return "int";
        else
            return "bool";
    }
}
