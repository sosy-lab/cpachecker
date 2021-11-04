// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


interface AbstractionState {

  /**
   * Determine on which variables to use abstraction when merging two
   * invariants states having this and the given abstraction state.
   *
   * @param pOther the other abstraction state.
   * @return the set of widening targets.
   */
  public Set<MemoryLocation> determineWideningTargets(AbstractionState pOther);

  public Set<BooleanFormula<CompoundInterval>> getWideningHints();

  public AbstractionState addEnteringEdge(CFAEdge pEdge);

  public AbstractionState join(AbstractionState pOther);

  public boolean isLessThanOrEqualTo(AbstractionState pOther);

}