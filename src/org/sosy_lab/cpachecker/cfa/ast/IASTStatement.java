package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTStatement extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTStatement {

  public IASTStatement(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTStatement copy() {
    throw new UnsupportedOperationException();
  }
}
