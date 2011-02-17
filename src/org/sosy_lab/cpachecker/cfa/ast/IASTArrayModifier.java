package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTArrayModifier extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTArrayModifier {

  private final IASTExpression expression;

  public IASTArrayModifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  @Override
  public IASTExpression getConstantExpression() {
    return expression;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {expression};
  }

  @Override
  @Deprecated
  public void setConstantExpression(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTArrayModifier copy() {
    throw new UnsupportedOperationException();
  }
}
