package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTIdExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTIdExpression {

  private final IASTName name;
  
  public IASTIdExpression(String pRawSignature, IASTFileLocation pFileLocation,
      IType pType, IASTName pName) {
    super(pRawSignature, pFileLocation, pType);
    name = pName;
  }

  @Override
  @Deprecated
  public int getRoleForName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  @Deprecated
  public void setName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTIdExpression copy() {
    throw new UnsupportedOperationException();
  }
}
