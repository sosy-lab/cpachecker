package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTLiteralExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTLiteralExpression {

  private final int kind;
  private final String value;
  
  public IASTLiteralExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      int pKind, String pValue) {
    super(pRawSignature, pFileLocation, pType);
    kind = pKind;
    value = pValue;
    assert pRawSignature.equals(pValue);
  }

  @Override
  public int getKind() {
    return kind;
  }

  @Override
  public char[] getValue() {
    return value.toCharArray();
  }

  @Override
  @Deprecated
  public void setKind(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setValue(char[] pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setValue(String pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTLiteralExpression copy() {
    throw new UnsupportedOperationException();
  }
}
