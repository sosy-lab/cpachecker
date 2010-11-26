package org.sosy_lab.cpachecker.cfa.ast;

public class IASTContinueStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTContinueStatement {

  public IASTContinueStatement(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTContinueStatement copy() {
    throw new UnsupportedOperationException();
  }
}
