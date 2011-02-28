package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTElaboratedTypeSpecifier extends IASTDeclSpecifier {

  private final int      kind;
  private final IASTName name;

  public IASTElaboratedTypeSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      boolean pConst, final boolean pInline, final boolean pVolatile,
      final int pKind, final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    kind = pKind;
    name = pName;
  }

  public int getKind() {
    return kind;
  }

  public IASTName getName() {
    return name;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {name};
  }

  public static final int k_enum = 0;
  public static final int k_struct = 1;
  public static final int k_union = 2;
  
}