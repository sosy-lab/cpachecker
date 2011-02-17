package org.sosy_lab.cpachecker.cfa.ast;

public class IASTGotoStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTGotoStatement {

  private final IASTName name;

  public IASTGotoStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTName pName) {
    super(pRawSignature, pFileLocation);
    name = pName;
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  public IASTNode[] getChildren() {
    return new IASTNode[] { name };
  }

  @Override
  @Deprecated
  public IASTGotoStatement copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }
}
