package org.sosy_lab.cpachecker.cfa.ast;

public final class ITypedef extends IBindingType {

  private final IType type;
  
  public ITypedef(final String pName, final IType pType) {
    super(pName);
    type = pType;
  }

  public IType getType() {
    return type;
  }

  @Override
  public boolean isSameType(final IType other) {
    return (other instanceof ITypedef) && ((ITypedef)other).getType() == getType();
  }

}
