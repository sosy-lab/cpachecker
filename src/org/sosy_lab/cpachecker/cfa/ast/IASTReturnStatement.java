package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

public class IASTReturnStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTReturnStatement {

  public IASTReturnStatement(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
    // TODO Auto-generated constructor stub
  }

  @Override
  @Deprecated
  public IASTExpression getReturnValue() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setReturnValue(IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTReturnStatement copy() {
    throw new UnsupportedOperationException();
  }
}
