package parser.types;

import typechecker.Substitution;

public class EmptyListType extends Type {

    private static EmptyListType instance = null;

    private EmptyListType(){

    }

    public static EmptyListType getInstance(){
        if(instance == null){
            instance = new EmptyListType();
        }
        return instance;
    }

    @Override
    public Type applySubstitution(Substitution substitution) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return getClass() == obj.getClass();
    }

    @Override
    public String toString() {
        return "Empty";
    }
}
