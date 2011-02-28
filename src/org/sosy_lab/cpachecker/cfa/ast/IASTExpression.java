package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTExpression extends IASTNode {

  private final IType type;

  public IASTExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType) {
    super(pRawSignature, pFileLocation);
    type = pType;
  }

  public IType getExpressionType() {
    return type;
  }
}
