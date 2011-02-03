package org.sosy_lab.cpachecker.cfa.ast;

public final class IPointerType extends IType implements
    org.eclipse.cdt.core.dom.ast.IPointerType {

  private final IType   type;
  private final boolean isConst;
  private final boolean isVolatile;

  public IPointerType(final IType pType, final boolean pIsConst,
      final boolean pIsVolatile) {
    type = pType;
    isConst = pIsConst;
    isVolatile = pIsVolatile;
  }

  @Override
  public IType getType() throws DOMException {
    return type;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

}
