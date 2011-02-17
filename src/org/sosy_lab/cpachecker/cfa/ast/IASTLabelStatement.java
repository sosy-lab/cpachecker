package org.sosy_lab.cpachecker.cfa.ast;

public class IASTLabelStatement extends IASTStatement implements
    org.eclipse.cdt.core.dom.ast.IASTLabelStatement {

  private final IASTName      name;
  private final IASTStatement nestedStatement;

  public IASTLabelStatement(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTName pName,
      final IASTStatement pNestedStatement) {
    super(pRawSignature, pFileLocation);
    name = pName;
    nestedStatement = pNestedStatement;
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  public IASTStatement getNestedStatement() {
    return nestedStatement;
  }

  @Override
  public IASTNode[] getChildren() {
    return new IASTNode[] { name, nestedStatement };
  }

  @Override
  @Deprecated
  public IASTLabelStatement copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setNestedStatement(
      final org.eclipse.cdt.core.dom.ast.IASTStatement pArg0) {
    throw new UnsupportedOperationException();
  }
}
