package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTNamedTypeSpecifier extends IASTDeclSpecifier implements
    org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier {

  private final IASTName name;

  public IASTNamedTypeSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
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
  public IASTNamedTypeSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
