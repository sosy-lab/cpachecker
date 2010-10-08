package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTDeclaration extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTDeclaration {

  public IASTDeclaration(String pRawSignature, IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  @Deprecated
  public IASTDeclaration copy() {
    throw new UnsupportedOperationException();
  }
}
