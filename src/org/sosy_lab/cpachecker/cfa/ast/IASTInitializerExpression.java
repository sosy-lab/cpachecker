package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTInitializerExpression extends IASTInitializer implements
    org.eclipse.cdt.core.dom.ast.IASTInitializerExpression {

  private final IASTExpression expression;
  
  public IASTInitializerExpression(String pRawSignature,
      IASTFileLocation pFileLocation, IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  @Override
  public IASTExpression getExpression() {
    return expression;
  }

  @Override
  @Deprecated
  public void setExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  @Deprecated
  public IASTInitializerExpression copy() {
    throw new UnsupportedOperationException();
  }

}
