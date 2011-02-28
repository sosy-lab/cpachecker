package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTDeclSpecifier extends IASTNode {

  private final int storageClass;
  private boolean   isConst;
  private boolean   isInline;
  private boolean   isVolatile;

  public IASTDeclSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile) {
    super(pRawSignature, pFileLocation);
    storageClass = pStorageClass;
    isConst = pConst;
    isInline = pInline;
    isVolatile = pVolatile;
  }

  public int getStorageClass() {
    return storageClass;
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isInline() {
    return isInline;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public IASTNode[] getChildren(){
    // there are no children of this class
    return new IASTNode[0];
  }

  public static final int sc_typedef = 1;
  public static final int sc_extern  = 2;
}
