package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTPointer extends IASTPointerOperator implements
    org.eclipse.cdt.core.dom.ast.IASTPointer {

  private final boolean isConst;
  private final boolean isVolatile;
  
  public IASTPointer(String pRawSignature, IASTFileLocation pFileLocation,
      boolean pIsConst, boolean pIsVolatile) {
    super(pRawSignature, pFileLocation);
    isConst = pIsConst;
    isVolatile = pIsVolatile;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  @Deprecated
  public void setConst(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setVolatile(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTPointer copy() {
    throw new UnsupportedOperationException();
  }
}
