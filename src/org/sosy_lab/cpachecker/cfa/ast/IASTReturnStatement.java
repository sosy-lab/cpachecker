package org.sosy_lab.cpachecker.cfa.ast;

public class IASTReturnStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTReturnStatement {

  private final IASTExpression expression;

  public IASTReturnStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  @Override
  public IASTNode[] getChildren() {
    return new IASTNode[] { expression };
  }

  @Override
  public IASTExpression getReturnValue() {
    return expression;
  }

  @Override
  @Deprecated
  public void setReturnValue(final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTReturnStatement copy() {
    throw new UnsupportedOperationException();
  }
}
