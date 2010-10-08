package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IBasicType extends IType implements
    org.eclipse.cdt.core.dom.ast.IBasicType {

  private final int type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  
  public IBasicType(int pType, boolean pIsLong, boolean pIsShort,
      boolean pIsSigned, boolean pIsUnsigned) {
    type = pType;
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
  }

  @Override
  public int getType() throws DOMException {
    return type;
  }

  @Override
  @Deprecated
  public IASTExpression getValue() throws DOMException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLong() throws DOMException {
    return isLong;
  }

  @Override
  public boolean isShort() throws DOMException {
    return isShort;
  }

  @Override
  public boolean isSigned() throws DOMException {
    return isSigned;
  }

  @Override
  public boolean isUnsigned() throws DOMException {
    return isUnsigned;
  }

}
