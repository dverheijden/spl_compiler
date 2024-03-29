package parser.types;

import typechecker.Substitution;

import java.util.Objects;

public class TupleType extends Type{
    public  Type left;
    public Type right;

    public TupleType(Type left, Type right){
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TupleType tupleType = (TupleType) o;
        return Objects.equals(left, tupleType.left) &&
                Objects.equals(right, tupleType.right);
    }

    @Override
    public int hashCode() {

        return Objects.hash(left, right);
    }

    @Override
    public Type applySubstitution(Substitution substitution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return String.format("Tuple(%s, %s)", left, right);
    }
}
