// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Interface to implement in order for an abstract state to be able to be over-approximated by a
 * formula representing the abstract state.
 */
public interface FormulaReportingState extends AbstractState {

  /** Returns a non-instantiated formula over-approximating the state. */
  BooleanFormula getFormulaApproximation(FormulaManagerView manager);
}
