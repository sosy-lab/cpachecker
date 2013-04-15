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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ISIOperator;

/**
 * The instance of this class is an operator for computing the remainder
 * of dividing a simple interval by a simple interval to produce another
 * simple interval.
 */
enum ModuloOperator implements IIIOperator {

  INSTANCE;

  /**
   * Computes a superset of the possible values resulting from calculating
   * for any value <code>a</code> of the first operand interval and any
   * value <code>b</code> of the second operand interval the operation
   * <code>a%b</code>.
   *
   * @param pFirstOperand the first operand, which is an interval that
   * contains the values to be divided by the second operand interval
   * values.
   * @param pSecondOperand the second operand interval which represents the
   * range of modulo divisors.
   * @return a superset of the possible results from calculating the modulo
   * operation between any value of the first operand interval as numerators
   * and any value of the second operand interval as divisors.
   */
  @Override
  public SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
    if (!pSecondOperand.hasLowerBound() || !pSecondOperand.hasUpperBound()) {
      return pFirstOperand;
    }
    return ISIOperator.MODULO_OPERATOR.apply(pFirstOperand, pSecondOperand.getLowerBound().abs().max(pSecondOperand.getUpperBound().abs()));
  }

}
