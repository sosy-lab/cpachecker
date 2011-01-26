package org.sosy_lab.cpachecker.cfa.ast;

public class IASTBinaryExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTBinaryExpression {

  private final IASTExpression operand1;
  private final IASTExpression operand2;
  private final int            operator;

  public IASTBinaryExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand1, final IASTExpression pOperand2,
      final int pOperator) {
    super(pRawSignature, pFileLocation, pType);
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOperator;
  }

  @Override
  public IASTExpression getOperand1() {
    return operand1;
  }

  @Override
  public IASTExpression getOperand2() {
    return operand2;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  @Deprecated
  public void setOperand1(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperand2(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperator(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTBinaryExpression copy() {
    throw new UnsupportedOperationException();
  }
}
