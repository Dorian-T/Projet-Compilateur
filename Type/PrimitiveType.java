package Type;
import java.util.Map;

public  class PrimitiveType extends Type {
    private Type.Base type; 
    
    /**
     * Constructeur
     * @param type type de base
     */
    public PrimitiveType(Type.Base type) {
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
        return (t instanceof PrimitiveType) && (((PrimitiveType)t).getType() == this.type);
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
     * Returns a string representation of the PrimitiveType.
     *
     * @return a string representation of the PrimitiveType
     */
    @Override
    public String toString() {
        if (type == Type.Base.INT)
            return "int";
        else
            return "bool";
    }
}
