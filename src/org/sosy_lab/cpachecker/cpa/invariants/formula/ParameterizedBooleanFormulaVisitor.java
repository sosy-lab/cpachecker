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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of implementing classes are visitors for boolean formulae that
 * accept an additional parameter to take into consideration on visiting a
 * formula.
 *
 * @param <ConstantType> the type of the constants used in the visited
 * formulae.
 * @param <ParameterType> the type of the additional parameter.
 * @param <ReturnType> the type of the visit results.
 */
public interface ParameterizedBooleanFormulaVisitor<ConstantType, ParameterType, ReturnType> {

  /**
   * Visits the given equation invariants formula.
   *
   * @param pEqual the equation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Equal<ConstantType> pEqual, ParameterType pParameter);

  /**
   * Visits the given less-than inequation invariants formula.
   *
   * @param pLessThan the less-than inequation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LessThan<ConstantType> pLessThan, ParameterType pParameter);

  /**
   * Visits the given logical conjunction invariants formula.
   *
   * @param pAnd the logical conjunction invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LogicalAnd<ConstantType> pAnd, ParameterType pParameter);

  /**
   * Visits the given logical negation invariants formula.
   *
   * @param pNot the logical negation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LogicalNot<ConstantType> pNot, ParameterType pParameter);

  ReturnType visitFalse(ParameterType pParameter);

  ReturnType visitTrue(ParameterType pParameter);

}
