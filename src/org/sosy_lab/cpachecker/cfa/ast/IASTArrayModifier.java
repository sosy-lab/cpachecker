package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTArrayModifier extends IASTNode {

  private final IASTExpression expression;

  public IASTArrayModifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  public IASTExpression getConstantExpression() {
    return expression;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {expression};
  }
}
