package org.sosy_lab.cpachecker.cfa.ast;

public class IASTLiteralExpression extends IASTExpression {

  private final int    kind;

  public IASTLiteralExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType, final int pKind) {
    super(pRawSignature, pFileLocation, pType);
    kind = pKind;
  }

  public int getKind() {
    return kind;
  }

  public char[] getValue() {
    return getRawSignature().toCharArray();
  }

  @Override
  public IASTNode[] getChildren(){
    // there are no children of this class
    return new IASTNode[0];
  }
  
  public static final int lk_integer_constant = 0;
  public static final int lk_float_constant = 1;
  public static final int lk_char_constant = 2;
  public static final int lk_string_literal = 3;

}
