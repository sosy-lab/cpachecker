// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Termination candidate for BMC termination mode.
 *
 * <p>Compared to {@link EdgeFormulaNegation}, this candidate is only evaluated on loop-bound
 * frontier states, i.e., on states that were cut due to the current loop bound.
 */
public final class FrontierEdgeFormulaNegation extends EdgeFormulaNegation {

  public FrontierEdgeFormulaNegation(CFANode pLocation, AssumeEdge pEdge) {
    super(pLocation, pEdge);
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return FluentIterable.from(super.filterApplicable(pStates))
        .filter(
            state -> {
              LoopBoundState loopBoundState =
                  AbstractStates.extractStateByType(state, LoopBoundState.class);
              return loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance();
            });
  }
}
