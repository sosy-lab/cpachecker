package org.sosy_lab.cpachecker.cfa.ast;

public class IASTBreakStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTBreakStatement {

  public IASTBreakStatement(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTBreakStatement copy() {
    throw new UnsupportedOperationException();
  }
}
