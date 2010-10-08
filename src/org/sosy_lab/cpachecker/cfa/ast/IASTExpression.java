package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTExpression extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTExpression {

  private final IType type;

  public IASTExpression(String pRawSignature, IASTFileLocation pFileLocation, IType pType) {
    super(pRawSignature, pFileLocation);
    type = pType;
  }
 
  @Override
  public IType getExpressionType() {
    return type;
  }

  @Override
  @Deprecated
  public IASTExpression copy() {
    throw new UnsupportedOperationException();
  }
}
