package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTNamedTypeSpecifier extends IASTDeclSpecifier implements
    org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier {

  private final IASTName name;
  
  public IASTNamedTypeSpecifier(String pRawSignature,
      IASTFileLocation pFileLocation, int pStorageClass, boolean pConst,
      boolean pInline, boolean pVolatile, IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
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
  public IASTNamedTypeSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
