package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTArraySubscriptExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression {

  private final IASTExpression arrayExpression;
  private final IASTExpression subscriptExpression;
  
  public IASTArraySubscriptExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      IASTExpression pArrayExpression, IASTExpression pSubscriptExpression) {
    super(pRawSignature, pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;
  }

  @Override
  public IASTExpression getArrayExpression() {
    return arrayExpression;
  }

  @Override
  public IASTExpression getSubscriptExpression() {
    return subscriptExpression;
  }

  @Override
  @Deprecated
  public void setArrayExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setSubscriptExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTArraySubscriptExpression copy() {
    throw new UnsupportedOperationException();
  }
}
