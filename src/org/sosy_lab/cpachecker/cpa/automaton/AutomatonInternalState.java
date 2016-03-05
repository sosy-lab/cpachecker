/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Represents a State in the automaton.
 */
public class AutomatonInternalState {
  // the StateId is used to identify States in GraphViz
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int stateId = idGenerator.getFreshId();

  /** State representing BOTTOM */
  static final AutomatonInternalState BOTTOM = new AutomatonInternalState("_predefinedState_BOTTOM", Collections.<AutomatonTransition>emptyList());

  /** State representing TOP */
  static final AutomatonInternalState TOP = new AutomatonInternalState("_predefinedState_TOP", Collections.<AutomatonTransition>emptyList());

  /** State representing INACTIVE: an automata that is not considered any more (removed from the precision). */
  static final AutomatonInternalState INACTIVE = new AutomatonInternalState("_predefinedState_INACTIVE", AutomatonBoolExpr.TRUE);

  static final AutomatonInternalState INTERMEDIATEINACTIVE = new AutomatonInternalState(
      "_predefinedState_INTERMEDIATEINACTIVE",
      Collections.singletonList(new AutomatonTransition(
                                    AutomatonBoolExpr.TRUE,
                                    null,
                                    true,
                                    ImmutableList.<AAstNode>of(),
                                    ImmutableList.<AutomatonAction>of(),
                                    INACTIVE, ImmutableSet.<SafetyProperty>of())),
      false, false);

  /** Error State */
  static final AutomatonInternalState ERROR = new AutomatonInternalState(
      "_predefinedState_ERROR",
      Collections.singletonList(new AutomatonTransition(
                                    AutomatonBoolExpr.TRUE,
                                    null,
                                    true,
                                    ImmutableList.<AAstNode>of(),
                                    ImmutableList.<AutomatonAction>of(),
                                    BOTTOM, ImmutableSet.<SafetyProperty>of())),
      true, false);

  /** Break state, used to halt the analysis without being a target state */
  static final AutomatonInternalState BREAK = new AutomatonInternalState(
      "_predefinedState_BREAK",
      Collections.singletonList(new AutomatonTransition(
                                    AutomatonBoolExpr.TRUE,
                                    null,
                                    true,
                                    ImmutableList.<AAstNode>of(),
                                    ImmutableList.<AutomatonAction>of(),
                                    BOTTOM, ImmutableSet.<SafetyProperty>of())),
      false, false);

  /** Name of this State.  */
  private final String name;
  /** Outgoing transitions of this state.  */
  private final ImmutableList<AutomatonTransition> transitions;

  private final boolean isTarget;

  /**
   * determines if all transitions of the state are considered or only the first that matches
   */
  private final boolean allTransitions;

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions, boolean pIsTarget, boolean pAllTransitions) {
    this.name = pName;
    this.transitions = ImmutableList.copyOf(pTransitions);
    this.isTarget = pIsTarget;
    this.allTransitions = pAllTransitions;
  }

  public AutomatonInternalState(String pName, AutomatonBoolExpr pSelfTransitionExpr) {
    this.name = pName;
    this.isTarget = false;
    this.allTransitions = false;
    this.transitions = ImmutableList.<AutomatonTransition>of(new AutomatonTransition(
        pSelfTransitionExpr,
        null,
        true,
        ImmutableList.<AAstNode>of(),
        Collections.<AutomatonAction>emptyList(),
        this,
        ImmutableSet.<SafetyProperty>of()));
  }

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions) {
    this(pName, pTransitions, false, false);
  }

  public boolean isNonDetState() {
    return allTransitions;
  }

  /** Lets all outgoing transitions of this state resolve their "sink" states.
   * @param pAllStates map of all states of this automaton.
   */
  void setFollowStates(Map<String, AutomatonInternalState> pAllStates) throws InvalidAutomatonException {
    for (AutomatonTransition t : transitions) {
      t.setFollowState(pAllStates);
    }
  }

  public String getName() {
    return name;
  }
  /** @return a integer representation of this state.
   */
  public int getStateId() {
    return stateId;
  }

  public boolean isTarget() {
    return isTarget;
  }

  /**
   * @return Is it a state in that we will remain
   *  the rest of the time?
   */
  public boolean isFinalSelfLoopingState() {
    if (transitions.size() == 1) {
      AutomatonTransition tr = transitions.get(0);
      if (tr.getFollowState().equals(this)) {
        return true;
      }
    }

    return false;
  }

  public boolean getDoesMatchAll() {
    return allTransitions;
  }

  public ImmutableList<AutomatonTransition> getTransitions() {
    return transitions;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + stateId;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    AutomatonInternalState other = (AutomatonInternalState) obj;

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }

    if (stateId != other.stateId) {
      return false;
    }

    return true;
  }

}
