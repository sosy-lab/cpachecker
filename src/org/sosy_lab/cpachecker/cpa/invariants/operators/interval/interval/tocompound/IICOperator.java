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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tocompound;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to two simple interval operands, producing a compound state
 * representing the result of the operation.
 */
public interface IICOperator extends Operator<SimpleInterval, SimpleInterval, CompoundInterval> {

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the first simple interval operand to apply the operator to.
   * @param pSecondOperand the second simple interval operand to apply the operator to.
   * @return the compound state resulting from applying the first operand to the
   * second operand.
   */
  @Override
  CompoundInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand);

  /**
   * The addition operator for adding intervals to other intervals.
   */
  Operator<SimpleInterval, SimpleInterval, CompoundInterval> ADD_OPERATOR = AddOperator.INSTANCE;
  /**
   * The division operator for dividing intervals by other intervals.
   */
  Operator<SimpleInterval, SimpleInterval, CompoundInterval> DIVIDE_OPERATOR = DivideOperator.INSTANCE;

  /**
   * The modulo operator for computing the remainders of dividing intervals by other intervals.
   */
  Operator<SimpleInterval, SimpleInterval, CompoundInterval> MODULO_OPERATOR = ModuloOperator.INSTANCE;

  /**
   * The multiplication operator for multiplying simple intervals with other intervals.
   */
  Operator<SimpleInterval, SimpleInterval, CompoundInterval> MULTIPLY_OPERATOR = MultiplyOperator.INSTANCE;

  /**
   * The left shift operator for left shifting simple intervals by other intervals.
   */
  Operator<SimpleInterval, SimpleInterval, CompoundInterval> SHIFT_LEFT_OPERATOR = ShiftLeftOperator.INSTANCE;

  /**
   * The right shift operator for right shifting simple intervals by other intervals.
   */
  Operator<SimpleInterval, SimpleInterval, CompoundInterval> SHIFT_RIGHT_OPERATOR = ShiftRightOperator.INSTANCE;

}
