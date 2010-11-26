package org.sosy_lab.cpachecker.cfa.ast;

public class IASTNullStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTNullStatement {

  public IASTNullStatement(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTNullStatement copy() {
    throw new UnsupportedOperationException();
  }
}
