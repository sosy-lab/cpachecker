package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IScope;

public final class ITypedef extends IType implements IBinding,
    org.eclipse.cdt.core.dom.ast.ITypedef {

  private final String name;
  private final IType type;
  
  public ITypedef(final String pName, final IType pType) {
    name = pName;
    type = pType;
  }

  @Override
  public ILinkage getLinkage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public char[] getNameCharArray() {
    return name.toCharArray();
  }

  @Override
  @Deprecated
  public IBinding getOwner() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IScope getScope() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("rawtypes")
  @Override
  @Deprecated
  public Object getAdapter(final Class pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IType getType() throws DOMException {
    return type;
  }

}
