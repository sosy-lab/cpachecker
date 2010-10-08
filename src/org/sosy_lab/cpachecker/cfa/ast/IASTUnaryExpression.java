package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTUnaryExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTUnaryExpression {

  private final IASTExpression operand;
  private final int operator;
    
  public IASTUnaryExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      IASTExpression pOperand, int pOperator) {
    super(pRawSignature, pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  @Override
  public IASTExpression getOperand() {
    return operand;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  @Deprecated
  public void setOperand(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperator(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTUnaryExpression copy() {
    throw new UnsupportedOperationException();
  }
}
