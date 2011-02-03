package org.sosy_lab.cpachecker.cfa.ast;

public class IASTExpressionStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTExpressionStatement {

  private final IASTExpression expression;

  public IASTExpressionStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  @Override
  public IASTExpression getExpression() {
    return expression;
  }

  @Override
  @Deprecated
  public void setExpression(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTExpressionStatement copy() {
    throw new UnsupportedOperationException();
  }
}
