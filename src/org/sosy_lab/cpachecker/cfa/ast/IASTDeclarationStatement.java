package org.sosy_lab.cpachecker.cfa.ast;

public class IASTDeclarationStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement {

  private final IASTDeclaration declaration;

  public IASTDeclarationStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTDeclaration pDeclaration) {
    super(pRawSignature, pFileLocation);
    declaration = pDeclaration;
  }

  @Override
  public IASTDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public IASTNode[] getChildren() {
    return new IASTNode[] { declaration };
  }

  @Override
  @Deprecated
  public void setDeclaration(
      final org.eclipse.cdt.core.dom.ast.IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTDeclarationStatement copy() {
    throw new UnsupportedOperationException();
  }
}
