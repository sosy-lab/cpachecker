/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators;

/**
 * Instances of implementations of this interface are operators that
 * can be applied to an operand of type O1 and an other operand of type
 * O2, producing a value of type R representing the result of the
 * operation.
 *
 * @param <O1> the type of the first operand.
 * @param <O2> the type of the second operand.
 * @param <R> the type of the result.
 */
public interface Operator<O1, O2, R> {

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the first operand to apply the operator to.
   * @param pSecondOperand the second operand to apply the operator to.
   * @return the value resulting from applying the first operand to the
   * second operand.
   */
  R apply(O1 operand1, O2 operand2);

}
