package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFieldReference extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTFieldReference {

  private final IASTName       name;
  private final IASTExpression owner;
  private final boolean        isPointerDereference;

  public IASTFieldReference(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTName pName, final IASTExpression pOwner,
      final boolean pIsPointerDereference) {
    super(pRawSignature, pFileLocation, pType);
    name = pName;
    owner = pOwner;
    isPointerDereference = pIsPointerDereference;
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName getFieldName() {
    return name;
  }

  @Override
  public IASTExpression getFieldOwner() {
    return owner;
  }

  @Override
  public boolean isPointerDereference() {
    return isPointerDereference;
  }

  @Override
  @Deprecated
  public void setFieldName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setFieldOwner(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setIsPointerDereference(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTFieldReference copy() {
    throw new UnsupportedOperationException();
  }
}
