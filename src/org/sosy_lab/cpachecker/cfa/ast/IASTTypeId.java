package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTypeId extends IASTNode implements org.eclipse.cdt.core.dom.ast.IASTTypeId {

  private final IASTDeclarator declarator;
  private final IASTDeclSpecifier specifier;
  
  public IASTTypeId(String pRawSignature, IASTFileLocation pFileLocation,
      IASTDeclarator pDeclarator, IASTDeclSpecifier pSpecifier) {
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
  @Deprecated
  public void setAbstractDeclarator(org.eclipse.cdt.core.dom.ast.IASTDeclarator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setDeclSpecifier(org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTTypeId copy() {
    throw new UnsupportedOperationException();
  }
}
