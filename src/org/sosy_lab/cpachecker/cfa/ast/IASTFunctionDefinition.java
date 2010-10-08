package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFunctionDefinition extends IASTDeclaration implements
    org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition {

  private final IASTDeclSpecifier specifier;
  private final IASTFunctionDeclarator declarator;
  
  public IASTFunctionDefinition(String pRawSignature,
      IASTFileLocation pFileLocation,
      IASTDeclSpecifier pSpecifier, IASTFunctionDeclarator pDeclarator) {
    super(pRawSignature, pFileLocation);
    specifier = pSpecifier;
    declarator = pDeclarator;
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IASTStatement getBody() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  @Override
  public IASTFunctionDeclarator getDeclarator() {
    return declarator;
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setBody(org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setDeclSpecifier(org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setDeclarator(org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTFunctionDefinition copy() {
    throw new UnsupportedOperationException();
  }
}
