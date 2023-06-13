// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.conditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Interface to implement in order for an abstract state to be able to make the system generate an
 * assumption to avoid re-considering this node.
 */
public interface AvoidanceReportingState extends AbstractState {

  /** Returns true if an invariant must be added so as to avoid the given state in the future. */
  boolean mustDumpAssumptionForAvoidance();

  /**
   * If {@link #mustDumpAssumptionForAvoidance()} returned true, this method returns a formula that
   * provides an explanation. This formula may not be TRUE. If the state cannot provide such a
   * formula, it SHOULD return FALSE. If {@link #mustDumpAssumptionForAvoidance()} returned false,
   * this method SHOULD return TRUE.
   */
  BooleanFormula getReasonFormula(FormulaManagerView mgr);
}
