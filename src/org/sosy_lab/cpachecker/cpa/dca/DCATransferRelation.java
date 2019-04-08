/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.cpa.dca;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.dca.bfautomaton.BFAutomatonState;
import org.sosy_lab.cpachecker.cpa.dca.bfautomaton.BFAutomatonTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DCATransferRelation extends SingleEdgeTransferRelation {

  private final DCACPA cpa;

  public DCATransferRelation(DCACPA pCpa) {
    cpa = pCpa;
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {
    Preconditions.checkArgument(pState instanceof DCAState);

    if (cpa.getAutomatonMap().isEmpty()) {
      return Collections.singleton(pState);
    }

    DCAState state = (DCAState) pState;

    if (state.getCompositeStates().stream().anyMatch(x -> x.getOutgoingTransitions().isEmpty())) {
      // shortcut -- don't return a successor state if there exists an automaton without an outgoing
      // transition
      return Collections.emptySet();
    }

    Set<DCAState> successors = Sets.newLinkedHashSetWithExpectedSize(2);
    // for (BFAutomatonState bfState : state.getCompositeStates()) {
    BFAutomatonState bfState = Iterables.getOnlyElement(state.getCompositeStates());
    for (BFAutomatonTransition bfTransition : bfState.getOutgoingTransitions()) {
      ImmutableList<BooleanFormula> bfAssumptions = bfTransition.getAssumptions();
      // DCAProperty violatedProperty = null;
      if (bfTransition.getFollowState().isAcceptingState()) {
        // TODO: create DCAProperty
      }
      successors.add(
          new DCAState(
              bfTransition.getFollowState().getName(), // statename
              ImmutableSet.of(bfTransition.getFollowState()),
              ImmutableSet.of(),
              bfAssumptions));
    }
    return successors;
  }

}
