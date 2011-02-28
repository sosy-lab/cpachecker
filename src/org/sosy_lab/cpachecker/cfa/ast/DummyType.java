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
  public boolean isSameType(final IType pArg0) {
    throw new UnsupportedOperationException();
  }
}
