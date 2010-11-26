package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTName;

public class IASTGotoStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTGotoStatement {

  public IASTGotoStatement(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  @Override
  public int getRoleForName(IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public IASTName getName() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setName(IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTGotoStatement copy() {
    throw new UnsupportedOperationException();
  }
}
