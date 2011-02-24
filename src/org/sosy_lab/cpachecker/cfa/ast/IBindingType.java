package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IScope;

public abstract class IBindingType extends IType implements IBinding {

  private final String name;
  
  public IBindingType(String pName) {
    name = pName;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  @Deprecated
  public Object getAdapter(Class pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public ILinkage getLinkage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @Deprecated
  public char[] getNameCharArray() {
    throw new UnsupportedOperationException();
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

}
