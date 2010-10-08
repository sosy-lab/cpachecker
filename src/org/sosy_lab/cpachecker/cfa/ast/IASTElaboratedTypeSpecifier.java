package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTElaboratedTypeSpecifier extends IASTDeclSpecifier implements
    org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier {

  private final int kind;
  private final IASTName name;
  
  public IASTElaboratedTypeSpecifier(String pRawSignature,
      IASTFileLocation pFileLocation, int pStorageClass, boolean pConst,
      boolean pInline, boolean pVolatile,
      int pKind, IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    kind = pKind;
    name = pName;
  }

  @Override
  @Deprecated
  public int getRoleForName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getKind() {
    return kind;
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  @Deprecated
  public void setKind(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTElaboratedTypeSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
