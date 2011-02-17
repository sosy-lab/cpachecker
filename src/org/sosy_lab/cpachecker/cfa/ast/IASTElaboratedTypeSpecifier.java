package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTElaboratedTypeSpecifier extends IASTDeclSpecifier
    implements org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier {

  private final int      kind;
  private final IASTName name;

  public IASTElaboratedTypeSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      boolean pConst, final boolean pInline, final boolean pVolatile,
      final int pKind, final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    kind = pKind;
    name = pName;
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
  public IASTNode[] getChildren(){
    return new IASTNode[] {name};
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setKind(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTElaboratedTypeSpecifier copy() {
    throw new UnsupportedOperationException();
  }
  
  public static final int k_enum = 0;
  public static final int k_struct = 1;
  public static final int k_union = 2;
  
}