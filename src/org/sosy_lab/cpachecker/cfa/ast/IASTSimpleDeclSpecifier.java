package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTSimpleDeclSpecifier extends IASTDeclSpecifier implements
    org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier {

  private final int type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  
  public IASTSimpleDeclSpecifier(String pRawSignature,
      IASTFileLocation pFileLocation, int pStorageClass, boolean pConst,
      boolean pInline, boolean pVolatile,
      int pType, boolean pIsLong, boolean pIsShort, boolean pIsSigned,
      boolean pIsUnsigned) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    type = pType;
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
  }

  @Override
  public int getType() {
    return type;
  }

  @Override
  public boolean isLong() {
    return isLong;
  }

  @Override
  public boolean isShort() {
    return isShort;
  }

  @Override
  public boolean isSigned() {
    return isSigned;
  }

  @Override
  public boolean isUnsigned() {
    return isUnsigned;
  }

  @Override
  @Deprecated
  public void setLong(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setShort(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setSigned(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setType(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setUnsigned(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTSimpleDeclSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
