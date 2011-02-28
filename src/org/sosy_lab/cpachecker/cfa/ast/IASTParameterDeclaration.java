package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTParameterDeclaration extends IASTNode {

  private final IASTDeclSpecifier specifier;
  private final IASTDeclarator    declarator;

  public IASTParameterDeclaration(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTDeclSpecifier pSpecifier,
      final IASTDeclarator pDeclarator) {
    super(pRawSignature, pFileLocation);
    specifier = pSpecifier;
    declarator = pDeclarator;
  }

  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  public IASTDeclarator getDeclarator() {
    return declarator;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {specifier, declarator};
  }
}
