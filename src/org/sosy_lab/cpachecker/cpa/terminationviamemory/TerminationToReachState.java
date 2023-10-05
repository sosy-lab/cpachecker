// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/** Tracks remembered, already seen states */
public class TerminationToReachState
    implements AbstractStateWithAssumptions, Graphable, AbstractQueryableState {
  private final ImmutableSet<PathFormula> assumptions;
  public TerminationToReachState(ImmutableSet<PathFormula> pAssumptions) {
    assumptions = pAssumptions;
  }
}
