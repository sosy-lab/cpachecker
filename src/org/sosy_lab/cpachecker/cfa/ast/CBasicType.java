package org.sosy_lab.cpachecker.cfa.ast;

public final class CBasicType extends IType {
  
  private final int     type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  private final boolean isComplex;
  private final boolean isImaginary;
  private final boolean isLongLong;
  
  public CBasicType(final int pType, final boolean pIsLong,
      final boolean pIsShort, final boolean pIsSigned,
      final boolean pIsUnsigned, final boolean pIsComplex,
      final boolean pIsImaginary, final boolean pIsLongLong) {

    type = pType;
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
    isComplex = pIsComplex;
    isImaginary = pIsImaginary;
    isLongLong = pIsLongLong;
  }

  public int getType() {
    return type;
  }

  public boolean isLong() {
    return isLong;
  }

  public boolean isShort() {
    return isShort;
  }

  public boolean isSigned() {
    return isSigned;
  }

  public boolean isUnsigned() {
    return isUnsigned;
  }

  public boolean isComplex() {
    return isComplex;
  }

  public boolean isImaginary() {
    return isImaginary;
  }

  public boolean isLongLong() {
    return isLongLong;
  }
 
  public static final int t_int = 3;
}
