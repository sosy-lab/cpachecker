package org.sosy_lab.cpachecker.cfa.ast;

public abstract class IASTDeclSpecifier extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier {

  private final int storageClass;
  private boolean   isConst;
  private boolean   isInline;
  private boolean   isVolatile;

  public IASTDeclSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile) {
    super(pRawSignature, pFileLocation);
    storageClass = pStorageClass;
    isConst = pConst;
    isInline = pInline;
    isVolatile = pVolatile;
  }

  @Override
  public int getStorageClass() {
    return storageClass;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isInline() {
    return isInline;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  @Deprecated
  public void setConst(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setInline(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setStorageClass(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setVolatile(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTDeclSpecifier copy() {
    throw new UnsupportedOperationException();
  }
}
