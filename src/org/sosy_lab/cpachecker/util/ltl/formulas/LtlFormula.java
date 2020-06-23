// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl.formulas;

import org.sosy_lab.cpachecker.util.ltl.LtlFormulaVisitor;

public interface LtlFormula {

  /**
   * Syntactically negate this formula.
   *
   * @return The negation of this formula in NNF.
   */
  LtlFormula not();

  String accept(LtlFormulaVisitor v);

  @Override
  int hashCode();

  @Override
  boolean equals(Object obj);

  @Override
  String toString();
}
