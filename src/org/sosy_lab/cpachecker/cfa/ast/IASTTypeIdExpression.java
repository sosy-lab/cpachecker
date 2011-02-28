package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTTypeIdExpression extends IASTExpression {

  private final int        operator;
  private final IASTTypeId type;

  public IASTTypeIdExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final int pOperator, final IASTTypeId pTypeId) {
    super(pRawSignature, pFileLocation, pType);
    operator = pOperator;
    type = pTypeId;
  }

  public int getOperator() {
    return operator;
  }

  public IASTTypeId getTypeId() {
    return type;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {type};
  }
 
  public static final int op_sizeof = 0;
  public static final int op_typeid = 1;
  public static final int op_alignof = 2;
  public static final int op_typeof = 3;
}
