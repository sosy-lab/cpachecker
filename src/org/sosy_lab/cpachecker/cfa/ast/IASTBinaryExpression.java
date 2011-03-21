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
  private final BinaryOperator operator;

  public IASTBinaryExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final IASTExpression pOperand1, final IASTExpression pOperand2,
      final BinaryOperator pOperator) {
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

  public BinaryOperator getOperator() {
    return operator;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {operand1, operand2};
  }

  public static enum BinaryOperator {
    MULTIPLY(false),
    DIVIDE(false),
    MODULO(false),
    PLUS(false),
    MINUS(false),
    SHIFT_LEFT(false),
    SHIFT_RIGHT(false),
    LESS_THAN(false),
    GREATER_THAN(false),
    LESS_EQUAL(false),
    GREATER_EQUAL(false),
    BINARY_AND(false),
    BINARY_XOR(false),
    BINARY_OR(false),
    LOGICAL_AND(false),
    LOGICAL_OR(false),
    ASSIGN(true),
    MULTIPLY_ASSIGN(true),
    DIVIDE_ASSIGN(true),
    MODULO_ASSIGN(true),
    PLUS_ASSIGN(true),
    MINUS_ASSIGN(true),
    SHIFT_LEFT_ASSIGN(true),
    SHIFT_RIGHT_ASSIGN(true),
    LESS_THAN_ASSIGN(true),
    GREATER_THAN_ASSIGN(true),
    LESS_EQUAL_ASSIGN(true),
    GREATER_EQUAL_ASSIGN(true),
    BINARY_AND_ASSIGN(true),
    BINARY_XOR_ASSIGN(true),
    BINARY_OR_ASSIGN(true),
    LOGICAL_AND_ASSIGN(true),
    LOGICAL_OR_ASSIGN(true),
    EQUALS(false),
    NOT_EQUALS(false),
    ;
    
    private final boolean isAssign;
    
    private BinaryOperator(boolean pIsAssign) {
      isAssign = pIsAssign;
    }
    
    /**
     * Returns true if this operator is some form of an assignment operator
     * (e.g. "=", "+=" etc.)
     */
    public boolean isAssign() {
      return isAssign;
    }
    
    public static BinaryOperator stripAssign(BinaryOperator op) {
      switch(op) {
      case MULTIPLY_ASSIGN:
        return BinaryOperator.MULTIPLY;
      case DIVIDE_ASSIGN:
        return DIVIDE;
      case MODULO_ASSIGN:
        return MODULO;
      case PLUS_ASSIGN:
        return BinaryOperator.PLUS;
      case MINUS_ASSIGN:
        return BinaryOperator.MINUS;
      case SHIFT_LEFT_ASSIGN:
        return BinaryOperator.SHIFT_LEFT;
      case SHIFT_RIGHT_ASSIGN:
        return BinaryOperator.SHIFT_RIGHT;
      case LESS_THAN_ASSIGN:
        return LESS_THAN;
      case GREATER_THAN_ASSIGN:
        return BinaryOperator.GREATER_THAN;
      case LESS_EQUAL_ASSIGN:
        return LESS_EQUAL;
      case GREATER_EQUAL_ASSIGN:
        return GREATER_EQUAL;
      case BINARY_AND_ASSIGN:
        return BinaryOperator.BINARY_AND;
      case BINARY_XOR_ASSIGN:
        return BinaryOperator.BINARY_XOR;
      case BINARY_OR_ASSIGN:
        return BinaryOperator.BINARY_OR;
      case LOGICAL_AND_ASSIGN:
        return LOGICAL_AND;
      case LOGICAL_OR_ASSIGN:
        return LOGICAL_OR;

      default:
        throw new IllegalArgumentException("Not an assigning operator");
      }
    }
  }
}
