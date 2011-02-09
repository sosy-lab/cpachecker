package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTSimpleDeclSpecifier extends IASTDeclSpecifier implements
    org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier {

  private final int     type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;

  public IASTSimpleDeclSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final int pType, final boolean pIsLong, final boolean pIsShort,
      final boolean pIsSigned, final boolean pIsUnsigned) {
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
  public void setLong(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setShort(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setSigned(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setType(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setUnsigned(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTSimpleDeclSpecifier copy() {
    throw new UnsupportedOperationException();
  }
  
  public static final int t_unspecified = 0;
  public static final int t_void = 1;
  public static final int t_char = 2;
  public static final int t_int = 3;
  public static final int t_float = 4;
  public static final int t_double = 5;

}
