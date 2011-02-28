package org.sosy_lab.cpachecker.cfa.ast;

public final class IPointerType extends IType {

  private final IType   type;
  private final boolean isConst;
  private final boolean isVolatile;

  public IPointerType(final IType pType, final boolean pIsConst,
      final boolean pIsVolatile) {
    type = pType;
    isConst = pIsConst;
    isVolatile = pIsVolatile;
  }

  public IType getType() {
    return type;
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public boolean isSameType(final IType other) {
    return (other instanceof IPointerType) && ((IPointerType)other).getType() == getType();
  }
}
