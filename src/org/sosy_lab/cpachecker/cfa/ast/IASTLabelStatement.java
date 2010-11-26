package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTName;

public class IASTLabelStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTLabelStatement {

  public IASTLabelStatement(String pRawSignature, IASTFileLocation pFileLocation) {
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
  public org.eclipse.cdt.core.dom.ast.IASTStatement getNestedStatement() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setName(IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setNestedStatement(
      org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTLabelStatement copy() {
    throw new UnsupportedOperationException();
  }
}
