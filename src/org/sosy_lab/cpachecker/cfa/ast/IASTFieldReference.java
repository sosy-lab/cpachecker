package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFieldReference extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTFieldReference {

  private final IASTName name;
  private final IASTExpression owner;
  private final boolean isPointerDereference;
  
  public IASTFieldReference(String pRawSignature,
      IASTFileLocation pFileLocation, IType pType,
      IASTName pName, IASTExpression pOwner, boolean pIsPointerDereference) {
    super(pRawSignature, pFileLocation, pType);
    name = pName;
    owner = pOwner;
    isPointerDereference = pIsPointerDereference;
  }

  @Override
  @Deprecated
  public int getRoleForName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
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
  public void setFieldName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setFieldOwner(org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setIsPointerDereference(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTFieldReference copy() {
    throw new UnsupportedOperationException();
  }
}
