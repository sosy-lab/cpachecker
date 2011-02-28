package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTCastExpression extends IASTExpression {

  private final IASTExpression operand;
  private final IASTTypeId     type;

  public IASTCastExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand, final IASTTypeId pTypeId) {
    super(pRawSignature, pFileLocation, pType);
    operand = pOperand;
    type = pTypeId;
  }

  public IASTExpression getOperand() {
    return operand;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {operand, type};
  }

  public IASTTypeId getTypeId() {
    return type;
  }
}
