package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTypeIdExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression {

  private final int operator;
  private final IASTTypeId type;
  
  public IASTTypeIdExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      int pOperator, IASTTypeId pTypeId) {
    super(pRawSignature, pFileLocation, pType);
    operator = pOperator;
    type = pTypeId;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  public IASTTypeId getTypeId() {
    return type;
  }

  @Override
  @Deprecated
  public void setOperator(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setTypeId(org.eclipse.cdt.core.dom.ast.IASTTypeId pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTTypeIdExpression copy() {
    throw new UnsupportedOperationException();
  }
}
