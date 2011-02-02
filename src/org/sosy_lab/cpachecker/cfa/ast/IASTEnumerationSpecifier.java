package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class IASTEnumerationSpecifier extends IASTDeclSpecifier implements
    org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier {

  private final List<IASTEnumerator> enumerators;
  private final IASTName             name;

  public IASTEnumerationSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final List<IASTEnumerator> pEnumerators, final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName;
  }

  @Override
  @Deprecated
  public int getRoleForName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void addEnumerator(
      final org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTEnumerator[] getEnumerators() {
    return enumerators.toArray(new IASTEnumerator[enumerators.size()]);
  }

  @Override
  public IASTName getName() {
    return name;
  }

  @Override
  @Deprecated
  public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTEnumerationSpecifier copy() {
    throw new UnsupportedOperationException();
  }

  public static final class IASTEnumerator extends IASTNode implements
      org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator {

    private final IASTName       name;
    private final IASTExpression value;

    public IASTEnumerator(final String pRawSignature,
        final IASTFileLocation pFileLocation, final IASTName pName,
        final IASTExpression pValue) {
      super(pRawSignature, pFileLocation);
      name = pName;
      value = pValue;
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
    public IASTExpression getValue() {
      return value;
    }

    @Override
    @Deprecated
    public void setName(final org.eclipse.cdt.core.dom.ast.IASTName pArg0) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setValue(final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public IASTEnumerator copy() {
      throw new UnsupportedOperationException();
    }
  }
}
