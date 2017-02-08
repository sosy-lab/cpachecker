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
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;

/** Represents a State in the automaton.
 */
public class AutomatonInternalState {
  // the StateId is used to identify States in GraphViz
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int stateId = idGenerator.getFreshId();

  /** State representing BOTTOM */
  static final AutomatonInternalState BOTTOM =
      new AutomatonInternalState("_predefinedState_BOTTOM", Collections.emptyList());

  /** Error State */
  static final AutomatonInternalState ERROR =
      new AutomatonInternalState(
          "_predefinedState_ERROR",
          Collections.singletonList(
              new AutomatonTransition(
                  AutomatonBoolExpr.TRUE,
                  Collections.emptyList(),
                  null,
                  Collections.emptyList(),
                  BOTTOM,
                  new StringExpression(""))),
          true,
          false);

  /** Break state, used to halt the analysis without being a target state */
  static final AutomatonInternalState BREAK =
      new AutomatonInternalState(
          "_predefinedState_BREAK",
          Collections.singletonList(
              new AutomatonTransition(
                  AutomatonBoolExpr.TRUE,
                  Collections.emptyList(),
                  null,
                  Collections.emptyList(),
                  BOTTOM,
                  null)),
          false,
          false);

  /** Name of this State.  */
  private final String name;
  /** Outgoing transitions of this state.  */
  private final List<AutomatonTransition> transitions;

  private final boolean mIsTarget;

  /**
   * determines if all transitions of the state are considered or only the first that matches
   */
  private final boolean mAllTransitions;

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions, boolean pIsTarget, boolean pAllTransitions) {
    this.name = pName;
    this.transitions = pTransitions;
    this.mIsTarget = pIsTarget;
    this.mAllTransitions = pAllTransitions;
  }

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions) {
    this(pName, pTransitions, false, false);
  }

  public boolean isNonDetState() {
    return mAllTransitions;
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
    return mIsTarget;
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
    return mAllTransitions;
  }

  public List<AutomatonTransition> getTransitions() {
    return transitions;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
