package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFieldReference extends IASTExpression {

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

  public IASTName getFieldName() {
    return name;
  }

  public IASTExpression getFieldOwner() {
    return owner;
  }

  public boolean isPointerDereference() {
    return isPointerDereference;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {name, owner};
  }
}
