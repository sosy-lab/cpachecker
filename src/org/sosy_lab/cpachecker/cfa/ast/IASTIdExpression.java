package org.sosy_lab.cpachecker.cfa.ast;

public class IASTIdExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTIdExpression {

  private final IASTName name;

  public IASTIdExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTName pName) {
    super(pRawSignature, pFileLocation, pType);
    name = pName;
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName getName() {
    return name;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {name};
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTIdExpression copy() {
    throw new UnsupportedOperationException();
  }
}
