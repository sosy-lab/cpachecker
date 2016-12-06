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

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeInstance.AutomatonPropertyScopeInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PropertyScopeState implements AbstractState, Graphable {

  private final long propertyDependantMatches;
  private final PersistentList<PropertyScopeState> prevBlockStates;
  private final CFAEdge enteringEdge;
  private final List<String> callstack;
  private final Set<ScopeLocation> scopeLocations;
  private final PropertyScopeState prevState;
  private final Map<Automaton, AutomatonState> automatonStates;
  private final Map<Automaton, AutomatonPropertyScopeInstance> automScopeInsts;
  private final AbstractionFormula afterGlobalInitAbsFormula;
  private final AbstractionFormula lastVarClassScopeAbsFormula;
  private final ImmutableSet<ScopeLocation> candidateScopeLocations; // since last abstraction state

  public static PropertyScopeState initial(CFANode pNode) {
    return new PropertyScopeState(
        PersistentLinkedList.of(),
        0,
        null,
        singletonList(pNode.getFunctionName()),
        emptySet(),
        null,
        Collections.emptyMap(),
        Collections.emptyMap(),
        null,
        null,
        ImmutableSet.of());
  }

  public PropertyScopeState(
      PersistentList<PropertyScopeState> pPrevBlockStates,
      long pPropertyDependantMatches,
      CFAEdge pEnteringEdge,
      List<String> pCallstack,
      Set<ScopeLocation> pScopeLocations,
      PropertyScopeState pPrevState,
      Map<Automaton, AutomatonState> pAutomatonStates,
      Map<Automaton, AutomatonPropertyScopeInstance> pAutomScopeInsts,
      AbstractionFormula pAfterGlobalInitAbsFormula,
      AbstractionFormula pLastVarClassScopeAbsFormula,
      ImmutableSet<ScopeLocation> pCandidateScopeLocations) {

    prevBlockStates = pPrevBlockStates;
    propertyDependantMatches = pPropertyDependantMatches;
    enteringEdge = pEnteringEdge;
    callstack = Collections.unmodifiableList(pCallstack);
    scopeLocations = Collections.unmodifiableSet(pScopeLocations);
    prevState = pPrevState;
    automatonStates = Collections.unmodifiableMap(pAutomatonStates);
    automScopeInsts = Collections.unmodifiableMap(pAutomScopeInsts);
    afterGlobalInitAbsFormula = pAfterGlobalInitAbsFormula;
    lastVarClassScopeAbsFormula = pLastVarClassScopeAbsFormula;
    candidateScopeLocations = pCandidateScopeLocations;
  }



  public PersistentList<PropertyScopeState> getPrevBlockStates() {
    return prevBlockStates;
  }

  public CFAEdge getEnteringEdge() {
    return enteringEdge;
  }

  public List<String> getCallstack() {
    return callstack;
  }

  public long getPropertyDependantMatches() {
    return propertyDependantMatches;
  }

  public Set<ScopeLocation> getScopeLocations() {
    return scopeLocations;
  }

  public Optional<PropertyScopeState> getPrevState() {
    return Optional.ofNullable(prevState);
  }

  public Map<Automaton, AutomatonState> getAutomatonStates() {
    return automatonStates;
  }

  public Map<Automaton, AutomatonPropertyScopeInstance> getAutomScopeInsts() {
    return automScopeInsts;
  }

  public ImmutableSet<ScopeLocation> getCandidateScopeLocations() {
    return candidateScopeLocations;
  }

  public AbstractionFormula getAfterGlobalInitAbsFormula() {
    return afterGlobalInitAbsFormula;
  }

  public AbstractionFormula getLastVarClassScopeAbsFormula() {
    return lastVarClassScopeAbsFormula;
  }

  public Stream<PropertyScopeState> prevStateStream() {
    Iterable<PropertyScopeState> iterable = () -> new Iterator<PropertyScopeState>() {
      PropertyScopeState state = PropertyScopeState.this;

      @Override
      public boolean hasNext() {
        return state.prevState != null;
      }

      @Override
      public PropertyScopeState next() {
        state = state.prevState;
        if (state == null) {
          throw new NoSuchElementException();
        }
        return state;
      }
    };

    return StreamSupport.stream(iterable.spliterator(), false);
  }

  @Override
  public String toDOTLabel() {
    return String.format("SCOPE %s\n%s", scopeLocations, automScopeInsts);

  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
