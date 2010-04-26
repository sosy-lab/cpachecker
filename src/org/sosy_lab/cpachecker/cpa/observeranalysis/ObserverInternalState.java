/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/** Represents a State in the observer automaton.
 * @author rhein
 */
class ObserverInternalState {
  static final List<ObserverTransition> emptyTransitionList = Collections.emptyList();

  // the StateId is used to identify States in GraphViz
  private static int stateIdCounter = 0;
  // stateIdCounter is incremented every time an instance of ObserverState is created.
  private int stateId = stateIdCounter++;
  
  /** Error State */
  static final ObserverInternalState ERROR = new ObserverInternalState("_predefindedState_ERROR", emptyTransitionList);

  /** State representing BOTTOM */
  static final ObserverInternalState BOTTOM = new ObserverInternalState("_predefinedState_BOTTOM", emptyTransitionList); 
  
  /** Name of this State.  */
  private String name;
  /** Outgoing transitions of this state.  */
  private List<ObserverTransition> transitions;
  
  public ObserverInternalState(String pName, List<ObserverTransition> pTransitions) {
    this.name = pName;
    this.transitions = pTransitions;
  }
  
  /** Lets all outgoing transitions of this state resolve their "sink" states.
   * @param pAllStates map of all states of this automaton.
   */
  void setFollowStates(List<ObserverInternalState> pAllStates) throws InvalidAutomatonException {
    for (ObserverTransition t : transitions) {
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


  /**  Writes a representation of this state (as node) in DOT file format to the argument {@link PrintStream}.
   * @param pOut
   */
  public void writeTransitionsToDotFile(PrintStream pOut) {
    for (ObserverTransition t : transitions) {
      t.writeTransitionToDotFile(stateId, pOut);
    }
  }

  public List<ObserverTransition> getTransitions() {
    return transitions;
  }
  
  @Override
  public String toString() {
    return this.name;
  }
}
