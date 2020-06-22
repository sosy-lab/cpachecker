package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class Type {
    private String typeName;

    public Type(String name) {
        typeName = name;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Type) {
            Type other = (Type) o;
            return typeName.equals(other.getTypeName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17 * typeName.hashCode();
    }

    @Override
    public String toString() {
        return typeName;
    }
}
