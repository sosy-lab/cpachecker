package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

public class IASTWhileStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTWhileStatement {

  public IASTWhileStatement(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IASTStatement getBody() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTExpression getCondition() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setBody(org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setCondition(IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTWhileStatement copy() {
    throw new UnsupportedOperationException();
  }
}
