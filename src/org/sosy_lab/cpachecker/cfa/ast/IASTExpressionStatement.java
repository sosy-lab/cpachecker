package org.sosy_lab.cpachecker.cfa.ast;

public class IASTExpressionStatement extends IASTStatement {

  private final IASTExpression expression;

  public IASTExpressionStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  public IASTExpression getExpression() {
    return expression;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {expression};
  }
}
