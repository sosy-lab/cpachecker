// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of extending classes are visitors for invariants formulae which use a generic visit
 * method that can be used to handle general cases while special cases can be overridden.
 *
 * @param <ConstantType> the type of the constants used in the visited formulae.
 * @param <ReturnType> the type of the visit return values.
 */
abstract class DefaultBooleanFormulaVisitor<ConstantType, ReturnType>
    implements BooleanFormulaVisitor<ConstantType, ReturnType> {

  /**
   * Provides a generic visit method that can be applied to any invariants formula type.
   *
   * @param pFormula the visited formula.
   * @return the result of the generic visit.
   */
  protected abstract ReturnType visitDefault(BooleanFormula<ConstantType> pFormula);

  @Override
  public ReturnType visit(Equal<ConstantType> pEqual) {
    return visitDefault(pEqual);
  }

  @Override
  public ReturnType visit(LessThan<ConstantType> pLessThan) {
    return visitDefault(pLessThan);
  }

  @Override
  public ReturnType visit(LogicalAnd<ConstantType> pAnd) {
    return visitDefault(pAnd);
  }

  @Override
  public ReturnType visit(LogicalNot<ConstantType> pNot) {
    return visitDefault(pNot);
  }

  @Override
  public ReturnType visitFalse() {
    return visitDefault(BooleanConstant.<ConstantType>getFalse());
  }

  @Override
  public ReturnType visitTrue() {
    return visitDefault(BooleanConstant.<ConstantType>getTrue());
  }
}
