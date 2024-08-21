// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.Typed;

/**
 * Instances of implementing classes represent invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public interface NumeralFormula<ConstantType> extends Typed {

  /**
   * Accepts the given invariants formula visitor.
   *
   * @param pVisitor the visitor to accept.
   * @return the result computed by the visitor for this specific invariants formula.
   */
  <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor);

  /**
   * Accepts the given parameterized formula visitor.
   *
   * @param pVisitor the visitor to accept.
   * @param pParameter the parameter to be handed to the visitor for this visit.
   * @return the result computed by the visitor for this specific invariants formula.
   */
  <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter);
}
