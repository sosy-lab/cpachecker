package org.sosy_lab.cpachecker.cfa.ast;

public class IASTUnaryExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTUnaryExpression {

  private final IASTExpression operand;
  private final int            operator;

  public IASTUnaryExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand, final int pOperator) {
    super(pRawSignature, pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  @Override
  public IASTExpression getOperand() {
    return operand;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  @Deprecated
  public void setOperand(final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperator(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTUnaryExpression copy() {
    throw new UnsupportedOperationException();
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
