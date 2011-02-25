package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IType implements org.eclipse.cdt.core.dom.ast.IType {

  @Override
  public abstract boolean isSameType(final org.eclipse.cdt.core.dom.ast.IType pArg0);

  @Override
  @Deprecated
  public Object clone() {
    throw new UnsupportedOperationException();
  }
}
