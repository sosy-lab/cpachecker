// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

public interface BooleanFormula<ConstantType> {

  <ReturnType> ReturnType accept(BooleanFormulaVisitor<ConstantType, ReturnType> pVisitor);

  /**
   * Accepts the given parameterized formula visitor.
   *
   * @param pVisitor the visitor to accept.
   * @param pParameter the parameter to be handed to the visitor for this visit.
   * @return the result computed by the visitor for this specific boolean formula.
   */
  <ReturnType, ParamType> ReturnType accept(
      ParameterizedBooleanFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter);
}
