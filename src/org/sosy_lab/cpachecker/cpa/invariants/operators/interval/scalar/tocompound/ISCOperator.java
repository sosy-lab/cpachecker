/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tocompound;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to a simple interval and a scalar value, producing a
 * compound state representing the result of the operation.
 */
public interface ISCOperator extends Operator<SimpleInterval, BigInteger, CompoundState> {

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the simple interval operand to apply the operator to.
   * @param pSecondOperand the scalar operand to apply the operator to.
   * @return the compound state resulting from applying the first operand to the
   * second operand.
   */
  @Override
  CompoundState apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand);

  /**
   * The addition operator for adding scalar values to simple intervals.
   */
  Operator<SimpleInterval, BigInteger, CompoundState> ADD_OPERATOR = AddOperator.INSTANCE;

  /**
   * The division operator for dividing simple intervals by scalar values.
   */
  Operator<SimpleInterval, BigInteger, CompoundState> DIVIDE_OPERATOR = DivideOperator.INSTANCE;

  /**
   * The modulo operator for computing the remainders of dividing intervals by scalar values.
   */
  Operator<SimpleInterval, BigInteger, CompoundState> MODULO_OPERATOR = ModuloOperator.INSTANCE;

  /**
   * The multiplication operator for multiplying simple intervals with scalar values.
   */
  Operator<SimpleInterval, BigInteger, CompoundState> MULTIPLY_OPERATOR = MultiplyOperator.INSTANCE;

  /**
   * The left shift operator for left shifting simple intervals by scalar values.
   */
  Operator<SimpleInterval, BigInteger, CompoundState> SHIFT_LEFT_OPERATOR = ShiftLeftOperator.INSTANCE;

  /**
   * The right shift operator for right shifting simple intervals by scalar values.
   */
  Operator<SimpleInterval, BigInteger, CompoundState> SHIFT_RIGHT_OPERATOR = ShiftRightOperator.INSTANCE;

}
