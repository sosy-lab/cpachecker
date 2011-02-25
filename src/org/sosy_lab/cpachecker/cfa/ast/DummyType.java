package org.sosy_lab.cpachecker.cfa.ast;

class DummyType extends IType {

  private final String typeName;

  public DummyType(String pTypeName) {
    typeName = pTypeName;
  }

  @Override
  public String toString() {
    return typeName;
  }

  @Override
  public boolean isSameType(final org.eclipse.cdt.core.dom.ast.IType pArg0) {
    throw new UnsupportedOperationException();
  }
}
