package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTArrayModifier extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTArrayModifier {

  private final IASTExpression expression;
  
  public IASTArrayModifier(String pRawSignature, IASTFileLocation pFileLocation,
      IASTExpression pExpression) {
    super(pRawSignature, pFileLocation);
    expression = pExpression;
  }

  @Override
  public IASTExpression getConstantExpression() {
    return expression;
  }

  @Override
  @Deprecated
  public void setConstantExpression(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTArrayModifier copy() {
    throw new UnsupportedOperationException();
  }
}
