package org.sosy_lab.cpachecker.cfa.ast;

public final class CBasicType extends IType implements
    org.eclipse.cdt.core.dom.ast.c.ICBasicType {
  
  private final int     type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  private final boolean isComplex;
  private final boolean isImaginary;
  private final boolean isLongLong;
  
  public CBasicType(final int pType, final boolean pIsLong,
      final boolean pIsShort, final boolean pIsSigned, final boolean pIsUnsigned,
      boolean pIsComplex, boolean pIsImaginary, boolean pIsLongLong) {

    type = pType;
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
    isComplex = pIsComplex;
    isImaginary = pIsImaginary;
    isLongLong = pIsLongLong;
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
  public boolean isComplex() {
    return isComplex;
  }

  @Override
  public boolean isImaginary() {
    return isImaginary;
  }

  @Override
  public boolean isLongLong() {
    return isLongLong;
  }
  
  @Override
  public boolean isSameType(final org.eclipse.cdt.core.dom.ast.IType other) {
    return (other instanceof CBasicType) && ((CBasicType)other).getType() == getType();
  }
  
  @Override
  @Deprecated
  public IASTExpression getValue() {
    throw new UnsupportedOperationException();
  }
  
  public static final int t_int = 3;

}
