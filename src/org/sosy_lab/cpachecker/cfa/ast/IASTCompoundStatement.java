package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTCompoundStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTStatement {

  public IASTCompoundStatement(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  public IASTStatement[] getStatements() {
    throw new UnsupportedOperationException();
  }
}
