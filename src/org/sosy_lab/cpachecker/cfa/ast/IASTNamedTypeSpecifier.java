package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTNamedTypeSpecifier extends IASTDeclSpecifier {

  private final IASTName name;

  public IASTNamedTypeSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    name = pName;
  }

  public IASTName getName() {
    return name;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {name};
  }
}
