package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public class IASTCompoundStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTCompoundStatement {

  private final List<IASTStatement> statements;

  public IASTCompoundStatement(String pRawSignature,
      IASTFileLocation pFileLocation, List<IASTStatement> pList) {
    super(pRawSignature, pFileLocation);
    statements = pList;
  }

  @Override
  public IASTNode[] getChildren() {
    // there are no children of this class
    return getStatements();
  }

  @Override
  public IASTStatement[] getStatements() {
    return statements.toArray(new IASTStatement[statements.size()]);
  }

  @Override
  @Deprecated
  public IASTCompoundStatement copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addStatement(org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getScope() {
    throw new UnsupportedOperationException();
  }
}
