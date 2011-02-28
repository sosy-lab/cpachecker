package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTPointer extends IASTNode {

  private final boolean isConst;
  private final boolean isVolatile;

  public IASTPointer(final String pRawSignature,
      final IASTFileLocation pFileLocation, final boolean pIsConst,
      final boolean pIsVolatile) {
    super(pRawSignature, pFileLocation);
    isConst = pIsConst;
    isVolatile = pIsVolatile;
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public IASTNode[] getChildren(){
    // there are no children of this class
    return new IASTNode[0];
  }
}

