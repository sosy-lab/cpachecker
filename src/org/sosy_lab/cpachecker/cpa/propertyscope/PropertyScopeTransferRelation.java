/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.propertyscope;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeUtil.*;

import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

class PropertyScopeTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  public PropertyScopeTransferRelation(LogManager logger) {
    this.logger = logger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    PropertyScopeState state = (PropertyScopeState) pState;

    PersistentList<PropertyScopeState> prevBlkStates = state.getPrevBlockStates();
    PersistentList<PropertyScopeState> newPrevBlkStates = prevBlkStates.with(state);

    PropertyScopeState newState = new PropertyScopeState(newPrevBlkStates,
        -1, cfaEdge, emptyList(), emptySet(), Optional.of(state));
    return Collections.singleton(newState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {

    PropertyScopeState state = (PropertyScopeState) pState;
    CallstackState callstackState = otherStates.stream()
        .filter(CallstackState.class::isInstance)
        .map(CallstackState.class::cast)
        .findFirst().orElseThrow(() -> new CPATransferException("No Callstack state!"));

    List<String> callstack = getStack(callstackState);
    Set<ScopeLocation> scopeLocations = new LinkedHashSet<>(state.getScopeLocations());

    // automaton matches here -> in scope
    int propDepMatches = otherStates.stream()
        .filter(AutomatonState.class::isInstance)
        .map(AutomatonState.class::cast)
        .map(AutomatonState::getPropertyDependantMatches)
        .reduce(0, Integer::sum);
    state.getPrevState()
        .map(PropertyScopeState::getPropertyDependantMatches)
        .filter(oldMatches -> propDepMatches > oldMatches).ifPresent(p -> scopeLocations.add(
           new ScopeLocation(cfaEdge, callstack, Reason.AUTOMATON_MATCH)));

    return Collections.singleton(
        new PropertyScopeState(state.getPrevBlockStates(), propDepMatches,
            state.getEnteringEdge(), callstack, scopeLocations, state.getPrevState()));

  }
}
