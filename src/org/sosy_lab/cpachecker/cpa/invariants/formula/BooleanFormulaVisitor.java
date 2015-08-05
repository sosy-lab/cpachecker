/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.formula;


public interface BooleanFormulaVisitor<ConstantType, ReturnType> {

  /**
   * Visits the boolean constant {@code false}.
   *
   * @return the result of the visit.
   */
  ReturnType visitFalse();

  /**
   * Visits the boolean constant {@code true}.
   *
   * @return the result of the visit.
   */
  ReturnType visitTrue();

  /**
   * Visits the given equation invariants formula.
   *
   * @param pEqual the equation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Equal<ConstantType> pEqual);

  /**
   * Visits the given less-than inequation invariants formula.
   *
   * @param pLessThan the less-than inequation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LessThan<ConstantType> pLessThan);

  /**
   * Visits the given logical conjunction invariants formula.
   *
   * @param pAnd the logical conjunction invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LogicalAnd<ConstantType> pAnd);

  /**
   * Visits the given logical negation invariants formula.
   *
   * @param pNot the logical negation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LogicalNot<ConstantType> pNot);

}
