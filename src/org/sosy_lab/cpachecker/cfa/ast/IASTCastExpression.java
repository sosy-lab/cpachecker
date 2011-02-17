package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTCastExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTCastExpression {

  private final IASTExpression operand;
  private final IASTTypeId     type;

  public IASTCastExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand, final IASTTypeId pTypeId) {
    super(pRawSignature, pFileLocation, pType);
    operand = pOperand;
    type = pTypeId;
  }

  @Override
  public IASTExpression getOperand() {
    return operand;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {operand, type};
  }

  @Override
  @Deprecated
  public int getOperator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTTypeId getTypeId() {
    return type;
  }

  @Override
  @Deprecated
  public void setOperand(final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperator(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setTypeId(final org.eclipse.cdt.core.dom.ast.IASTTypeId pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTCastExpression copy() {
    throw new UnsupportedOperationException();
  }
}
