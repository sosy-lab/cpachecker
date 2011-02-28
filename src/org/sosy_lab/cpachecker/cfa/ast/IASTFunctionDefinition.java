package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFunctionDefinition extends IASTDeclaration {

  private final IASTDeclSpecifier      specifier;
  private final IASTFunctionDeclarator declarator;

  public IASTFunctionDefinition(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTDeclSpecifier pSpecifier,
      final IASTFunctionDeclarator pDeclarator) {
    super(pRawSignature, pFileLocation);
    specifier = pSpecifier;
    declarator = pDeclarator;
  }

  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  public IASTFunctionDeclarator getDeclarator() {
    return declarator;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {specifier, declarator};
  }
}
