// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public final class FrontierEdgeFormula extends EdgeFormula {

  public FrontierEdgeFormula(CFANode pLocation, AssumeEdge pEdge) {
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
