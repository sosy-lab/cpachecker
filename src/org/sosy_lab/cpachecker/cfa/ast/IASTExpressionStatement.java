package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

public final class IASTExpressionStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTExpressionStatement {

  public IASTExpressionStatement(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTExpressionStatement copy() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public IASTExpression getExpression() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setExpression(IASTExpression expression) {
    throw new UnsupportedOperationException();
  }
}
