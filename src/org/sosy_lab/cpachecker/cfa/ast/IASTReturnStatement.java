package org.sosy_lab.cpachecker.cfa.ast;

public class IASTReturnStatement extends IASTStatement {

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

  public IASTExpression getReturnValue() {
    return expression;
  }
}
