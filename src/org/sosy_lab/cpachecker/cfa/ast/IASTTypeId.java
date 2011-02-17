package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTypeId extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTTypeId {

  private final IASTDeclarator    declarator;
  private final IASTDeclSpecifier specifier;

  public IASTTypeId(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTDeclarator pDeclarator,
      final IASTDeclSpecifier pSpecifier) {
    super(pRawSignature, pFileLocation);
    declarator = pDeclarator;
    specifier = pSpecifier;
  }

  @Override
  public IASTDeclarator getAbstractDeclarator() {
    return declarator;
  }

  @Override
  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {declarator, specifier};
  }

  @Override
  @Deprecated
  public void setAbstractDeclarator(
      final org.eclipse.cdt.core.dom.ast.IASTDeclarator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setDeclSpecifier(
      final org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTTypeId copy() {
    throw new UnsupportedOperationException();
  }
}
