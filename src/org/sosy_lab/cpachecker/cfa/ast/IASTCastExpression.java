package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTCastExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTCastExpression {

  private final IASTExpression operand;
  private final IASTTypeId type;
  
  public IASTCastExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      IASTExpression pOperand, IASTTypeId pTypeId) {
    super(pRawSignature, pFileLocation, pType);
    operand = pOperand;
    type = pTypeId;
  }

  @Override
  public IASTExpression getOperand() {
    return operand;
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
  public void setTypeId(org.eclipse.cdt.core.dom.ast.IASTTypeId pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTCastExpression copy() {
    throw new UnsupportedOperationException();
  }
}
