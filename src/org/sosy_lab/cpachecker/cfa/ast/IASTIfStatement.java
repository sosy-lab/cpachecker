package org.sosy_lab.cpachecker.cfa.ast;

public class IASTIfStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTIfStatement {

  private final IASTExpression condition;
  private final IASTStatement  thenStatement;
  private final IASTStatement  elseStatement;

  public IASTIfStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTExpression pCondition,
      final IASTStatement pThenStatement, final IASTStatement pElseStatement) {
    super(pRawSignature, pFileLocation);
    condition = pCondition;
    thenStatement = pThenStatement;
    elseStatement = pElseStatement;
  }

  @Override
  public IASTExpression getConditionExpression() {
    return condition;
  }

  @Override
  public org.eclipse.cdt.core.dom.ast.IASTStatement getElseClause() {
    return elseStatement;
  }

  @Override
  public org.eclipse.cdt.core.dom.ast.IASTStatement getThenClause() {
    return thenStatement;
  }

  @Override
  public IASTNode[] getChildren() {
    return new IASTNode[] { condition, thenStatement, elseStatement };
  }

  @Override
  @Deprecated
  public void setConditionExpression(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setElseClause(
      final org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setThenClause(
      final org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTIfStatement copy() {
    throw new UnsupportedOperationException();
  }
}
