// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

interface BooleanFormulaVisitor<ConstantType, ReturnType> {

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
   * @return the result of the visit.
   */
  ReturnType visit(Equal<ConstantType> pEqual);

  /**
   * Visits the given less-than inequation invariants formula.
   *
   * @param pLessThan the less-than inequation invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(LessThan<ConstantType> pLessThan);

  /**
   * Visits the given logical conjunction invariants formula.
   *
   * @param pAnd the logical conjunction invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(LogicalAnd<ConstantType> pAnd);

  /**
   * Visits the given logical negation invariants formula.
   *
   * @param pNot the logical negation invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(LogicalNot<ConstantType> pNot);
}
