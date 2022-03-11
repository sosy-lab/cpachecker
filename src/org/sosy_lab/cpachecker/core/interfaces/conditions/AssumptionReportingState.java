// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.conditions;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * Interface to implement in order for an object to be able to contribute invariants to the
 * invariant construction.
 */
public interface AssumptionReportingState {

  /**
   * Get the assumptions that the given abstract state wants to report for its containing node's
   * location.
   *
   * @return a (possibly empty) list of assumptions representing the assumptions to generate for the
   *     given state
   */
  List<CExpression> getAssumptions();
}
