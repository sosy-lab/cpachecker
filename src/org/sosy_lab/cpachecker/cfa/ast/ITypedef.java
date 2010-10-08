package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.core.runtime.CoreException;

public final class ITypedef extends IType implements IBinding,
    org.eclipse.cdt.core.dom.ast.ITypedef {

  private final String name;
  private final IType type;
  
  public ITypedef(String pName, IType pType) {
    name = pName;
    type = pType;
  }

  @Override
  public ILinkage getLinkage() throws CoreException {
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
  public org.eclipse.cdt.core.dom.ast.IBinding getOwner() throws DOMException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getScope() throws DOMException {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("rawtypes")
  @Override
  @Deprecated
  public Object getAdapter(Class pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IType getType() throws DOMException {
    return type;
  }

}
