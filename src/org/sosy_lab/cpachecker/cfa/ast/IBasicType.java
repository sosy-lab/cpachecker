package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IBasicType extends IType implements
    org.eclipse.cdt.core.dom.ast.IBasicType {

  private final int     type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;

  public IBasicType(final int pType, final boolean pIsLong,
      final boolean pIsShort, final boolean pIsSigned, final boolean pIsUnsigned) {
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
  @Deprecated
  public IASTExpression getValue() {
    throw new UnsupportedOperationException();
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

  public static final int t_int = 3;

}
