// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/** Class for creating {@link Formula}s out of {@link Constraint}s */
public interface FormulaCreator {

  /**
   * Creates a {@link BooleanFormula} representing the given {@link Constraint}.
   *
   * @param pConstraint the constraint to create a formula of
   * @return a <code>Formula</code> representing the given constraint
   */
  BooleanFormula createFormula(Constraint pConstraint)
      throws UnrecognizedCodeException, InterruptedException;
}
