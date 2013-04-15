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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to a simple interval operand and a big integer operand,
 * producing another simple interval representing the result of the operation.
 */
public interface ISIOperator extends Operator<SimpleInterval, BigInteger, SimpleInterval> {

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the simple interval operand to apply the operator to.
   * @param pSecondOperand the big integer operand to apply the operator to.
   * @return the simple interval resulting from applying the first operand to the
   * second operand.
   */
  @Override
  SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand);

  /**
   * The addition operator for adding intervals to big integers.
   */
  Operator<SimpleInterval, BigInteger, SimpleInterval> ADD_OPERATOR = AddOperator.INSTANCE;

  /**
   * The multiplication operator for multiplying intervals with big integers.
   */
  Operator<SimpleInterval, BigInteger, SimpleInterval> MULTIPLY_OPERATOR = MultiplyOperator.INSTANCE;

  /**
   * The division operator for dividing intervals by big integers.
   */
  Operator<SimpleInterval, BigInteger, SimpleInterval> DIVIDE_OPERATOR = DivideOperator.INSTANCE;

  /**
   * The modulo operator for computing the remainder of dividing intervals
   * by big integers.
   */
  Operator<SimpleInterval, BigInteger, SimpleInterval> MODULO_OPERATOR = ModuloOperator.INSTANCE;

  /**
   * The left shift operator for left shifting a simple interval by a big
   * integer value.
   */
  Operator<SimpleInterval, BigInteger, SimpleInterval> SHIFT_LEFT_OPERATOR = ShiftLeftOperator.INSTANCE;

  /**
   * The right shift operator for right shifting a simple interval by a big
   * integer value.
   */
  Operator<SimpleInterval, BigInteger, SimpleInterval> SHIFT_RIGHT_OPERATOR = ShiftRightOperator.INSTANCE;
}
