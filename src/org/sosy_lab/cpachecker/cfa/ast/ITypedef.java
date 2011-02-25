package org.sosy_lab.cpachecker.cfa.ast;

public final class ITypedef extends IBindingType
        implements org.eclipse.cdt.core.dom.ast.ITypedef {

  private final IType type;
  
  public ITypedef(final String pName, final IType pType) {
    super(pName);
    type = pType;
  }

  @Override
  public IType getType() {
    return type;
  }

  @Override
  public boolean isSameType(final org.eclipse.cdt.core.dom.ast.IType other) {
    return (other instanceof ITypedef) && ((ITypedef)other).getType() == getType();
  }

}
