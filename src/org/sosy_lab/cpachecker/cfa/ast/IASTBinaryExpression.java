/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast;

public class IASTBinaryExpression extends IASTExpression {

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

  public IASTExpression getOperand1() {
    return operand1;
  }

  public IASTExpression getOperand2() {
    return operand2;
  }

  public int getOperator() {
    return operator;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {operand1, operand2};
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
