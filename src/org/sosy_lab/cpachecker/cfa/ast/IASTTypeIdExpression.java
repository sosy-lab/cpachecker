package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTypeIdExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression {

  private final int        operator;
  private final IASTTypeId type;

  public IASTTypeIdExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final int pOperator, final IASTTypeId pTypeId) {
    super(pRawSignature, pFileLocation, pType);
    operator = pOperator;
    type = pTypeId;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  public IASTTypeId getTypeId() {
    return type;
  }

  @Override
  @Deprecated
  public void setOperator(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setTypeId(final org.eclipse.cdt.core.dom.ast.IASTTypeId pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTTypeIdExpression copy() {
    throw new UnsupportedOperationException();
  }
}
