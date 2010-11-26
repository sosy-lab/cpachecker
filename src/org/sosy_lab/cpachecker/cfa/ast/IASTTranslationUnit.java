package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTranslationUnit extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTNode {

  public IASTTranslationUnit(String pRawSignature,
      IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Deprecated
  public IASTDeclaration[] getDeclarations() {
    throw new UnsupportedOperationException();
  }

}
