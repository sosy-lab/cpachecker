package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTInitializerExpression extends IASTInitializer {

  private final IASTExpression expression;

  public IASTInitializerExpression(final String pRawSignature,
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
