// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of implementing classes are visitors for boolean formulae that accept an additional
 * parameter to take into consideration on visiting a formula.
 *
 * @param <ConstantType> the type of the constants used in the visited formulae.
 * @param <ParameterType> the type of the additional parameter.
 * @param <ReturnType> the type of the visit results.
 */
interface ParameterizedBooleanFormulaVisitor<ConstantType, ParameterType, ReturnType> {

  /**
   * Visits the given equation invariants formula.
   *
   * @param pEqual the equation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Equal<ConstantType> pEqual, ParameterType pParameter);

  /**
   * Visits the given less-than inequation invariants formula.
   *
   * @param pLessThan the less-than inequation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(LessThan<ConstantType> pLessThan, ParameterType pParameter);

  /**
   * Visits the given logical conjunction invariants formula.
   *
   * @param pAnd the logical conjunction invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(LogicalAnd<ConstantType> pAnd, ParameterType pParameter);

  /**
   * Visits the given logical negation invariants formula.
   *
   * @param pNot the logical negation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(LogicalNot<ConstantType> pNot, ParameterType pParameter);

  ReturnType visitFalse(ParameterType pParameter);

  ReturnType visitTrue(ParameterType pParameter);
}
