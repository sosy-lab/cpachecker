package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTArraySubscriptExpression extends IASTExpression {

  private final IASTExpression arrayExpression;
  private final IASTExpression subscriptExpression;

  public IASTArraySubscriptExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pArrayExpression,
      final IASTExpression pSubscriptExpression) {
    super(pRawSignature, pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;
  }

  public IASTExpression getArrayExpression() {
    return arrayExpression;
  }

  public IASTExpression getSubscriptExpression() {
    return subscriptExpression;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {arrayExpression, subscriptExpression};
  }
}
