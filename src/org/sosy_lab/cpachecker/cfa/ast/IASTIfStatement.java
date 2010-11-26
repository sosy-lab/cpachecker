package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

public class IASTIfStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTIfStatement {

  public IASTIfStatement(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  @Override
  public IASTExpression getConditionExpression() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public org.eclipse.cdt.core.dom.ast.IASTStatement getElseClause() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public org.eclipse.cdt.core.dom.ast.IASTStatement getThenClause() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setConditionExpression(IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setElseClause(org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setThenClause(org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTIfStatement copy() {
    throw new UnsupportedOperationException();
  }
}
