package org.sosy_lab.cpachecker.cfa.ast;

public class IASTUnaryExpression extends IASTExpression {

  private final IASTExpression operand;
  private final int            operator;

  public IASTUnaryExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand, final int pOperator) {
    super(pRawSignature, pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  public IASTExpression getOperand() {
    return operand;
  }

  public int getOperator() {
    return operator;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {operand};
  }
  
  public static final int op_prefixIncr = 0;
  public static final int op_prefixDecr = 1;
  public static final int op_plus = 2;
  public static final int op_minus = 3;
  public static final int op_star = 4;
  public static final int op_amper = 5;
  public static final int op_tilde = 6;
  public static final int op_not = 7;
  public static final int op_sizeof = 8;
  public static final int op_postFixIncr = 9;
  public static final int op_postFixDecr = 10;
  public static final int op_bracketedPrimary = 11;
}
