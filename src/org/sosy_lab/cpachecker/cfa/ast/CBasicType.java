package org.sosy_lab.cpachecker.cfa.ast;

public final class CBasicType extends IBasicType implements
    org.eclipse.cdt.core.dom.ast.c.ICBasicType {

  private final boolean isComplex;
  private final boolean isImaginary;
  private final boolean isLongLong;
  
  public CBasicType(int pType, boolean pIsLong, boolean pIsShort,
      boolean pIsSigned, boolean pIsUnsigned,
      boolean pIsComplex, boolean pIsImaginary, boolean pIsLongLong) {
    super(pType, pIsLong, pIsShort, pIsSigned, pIsUnsigned);
    isComplex = pIsComplex;
    isImaginary = pIsImaginary;
    isLongLong = pIsLongLong;
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

}
