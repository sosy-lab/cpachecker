package org.sosy_lab.cpachecker.cfa.ast;

public class IASTLiteralExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTLiteralExpression {

  private final int    kind;

  public IASTLiteralExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType, final int pKind) {
    super(pRawSignature, pFileLocation, pType);
    kind = pKind;
  }

  @Override
  public int getKind() {
    return kind;
  }

  @Override
  public char[] getValue() {
    return getRawSignature().toCharArray();
  }

  @Override
  @Deprecated
  public void setKind(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setValue(final char[] pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setValue(final String pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTLiteralExpression copy() {
    throw new UnsupportedOperationException();
  }
  
  public static final int lk_integer_constant = 0;
  public static final int lk_float_constant = 1;
  public static final int lk_char_constant = 2;
  public static final int lk_string_literal = 3;

}
