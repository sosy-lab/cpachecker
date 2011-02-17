package org.sosy_lab.cpachecker.cfa.ast;

public class IASTBinaryExpression extends IASTExpression implements
    org.eclipse.cdt.core.dom.ast.IASTBinaryExpression {

  private final IASTExpression operand1;
  private final IASTExpression operand2;
  private final int            operator;

  public IASTBinaryExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand1, final IASTExpression pOperand2,
      final int pOperator) {
    super(pRawSignature, pFileLocation, pType);
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOperator;
  }

  @Override
  public IASTExpression getOperand1() {
    return operand1;
  }

  @Override
  public IASTExpression getOperand2() {
    return operand2;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {operand1, operand2};
  }

  @Override
  @Deprecated
  public void setOperand1(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperand2(
      final org.eclipse.cdt.core.dom.ast.IASTExpression pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setOperator(final int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTBinaryExpression copy() {
    throw new UnsupportedOperationException();
  }

  public static final int op_multiply         = 1;
  public static final int op_divide           = 2;
  public static final int op_modulo           = 3;
  public static final int op_plus             = 4;
  public static final int op_minus            = 5;
  public static final int op_shiftLeft        = 6;
  public static final int op_shiftRight       = 7;
  public static final int op_lessThan         = 8;
  public static final int op_greaterThan      = 9;
  public static final int op_lessEqual        = 10;
  public static final int op_greaterEqual     = 11;
  public static final int op_binaryAnd        = 12;
  public static final int op_binaryXor        = 13;
  public static final int op_binaryOr         = 14;
  public static final int op_logicalAnd       = 15;
  public static final int op_logicalOr        = 16;
  public static final int op_assign           = 17;
  public static final int op_multiplyAssign   = 18;
  public static final int op_divideAssign     = 19;
  public static final int op_moduloAssign     = 20;
  public static final int op_plusAssign       = 21;
  public static final int op_minusAssign      = 22;
  public static final int op_shiftLeftAssign  = 23;
  public static final int op_shiftRightAssign = 24;
  public static final int op_binaryAndAssign  = 25;
  public static final int op_binaryXorAssign  = 26;
  public static final int op_binaryOrAssign   = 27;
  public static final int op_equals           = 28;
  public static final int op_notequals        = 29;

}
