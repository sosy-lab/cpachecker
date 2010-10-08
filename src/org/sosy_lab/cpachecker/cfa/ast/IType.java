package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IType implements org.eclipse.cdt.core.dom.ast.IType {

  @Override
  public boolean isSameType(org.eclipse.cdt.core.dom.ast.IType pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public Object clone() {
    throw new UnsupportedOperationException();
  }
}
