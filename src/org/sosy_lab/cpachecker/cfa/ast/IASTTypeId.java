package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTypeId extends IASTNode {

  private final IASTDeclarator    declarator;
  private final IASTDeclSpecifier specifier;

  public IASTTypeId(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTDeclarator pDeclarator,
      final IASTDeclSpecifier pSpecifier) {
    super(pRawSignature, pFileLocation);
    declarator = pDeclarator;
    specifier = pSpecifier;
  }

  public IASTDeclarator getAbstractDeclarator() {
    return declarator;
  }

  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {declarator, specifier};
  }
}
