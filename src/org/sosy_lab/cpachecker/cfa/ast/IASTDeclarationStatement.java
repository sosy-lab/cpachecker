package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;

public class IASTDeclarationStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement {

  public IASTDeclarationStatement(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  @Override
  public IASTDeclaration getDeclaration() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public void setDeclaration(IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTDeclarationStatement copy() {
    throw new UnsupportedOperationException();
  }
}
