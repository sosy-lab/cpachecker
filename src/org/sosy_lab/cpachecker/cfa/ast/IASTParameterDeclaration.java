package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTParameterDeclaration extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration {

  private final IASTDeclSpecifier specifier;
  private final IASTDeclarator declarator;
  
  public IASTParameterDeclaration(String pRawSignature,
      IASTFileLocation pFileLocation,
      IASTDeclSpecifier pSpecifier, IASTDeclarator pDeclarator) {
    super(pRawSignature, pFileLocation);
    specifier = pSpecifier;
    declarator = pDeclarator;
  }

  @Override
  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  @Override
  public IASTDeclarator getDeclarator() {
    return declarator;
  }

  @Override
  @Deprecated
  public void setDeclSpecifier(org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setDeclarator(org.eclipse.cdt.core.dom.ast.IASTDeclarator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTParameterDeclaration copy() {
    throw new UnsupportedOperationException();
  }
}
