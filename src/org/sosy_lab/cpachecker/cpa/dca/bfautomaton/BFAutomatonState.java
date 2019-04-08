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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.dca.bfautomaton;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

/**
 * A single state in {@link BFAutomaton}
 */
public class BFAutomatonState {

  private final String stateName;
  private final ImmutableList<BFAutomatonTransition> outgoingTransitions;
  private final boolean isAcceptingState;

  public BFAutomatonState(
      String pStateName,
      List<BFAutomatonTransition> pOutgoingTransitions,
      boolean pIsAcceptingState) {

    stateName = checkNotNull(pStateName);
    outgoingTransitions = ImmutableList.copyOf(pOutgoingTransitions);
    isAcceptingState = pIsAcceptingState;
  }

  /**
   * Lets all outgoing transitions of this state resolve their "sink" states.
   *
   * @param pAllStates map of all states of this automaton.
   */
  void setFollowStates(Map<String, BFAutomatonState> pAllStates) throws BFAutomatonException {
    for (BFAutomatonTransition t : outgoingTransitions) {
      t.setFollowState(pAllStates);
    }
  }

  public String getName() {
    return stateName;
  }

  public boolean isAcceptingState() {
    return isAcceptingState;
  }

  public ImmutableList<BFAutomatonTransition> getOutgoingTransitions() {
    return outgoingTransitions;
  }

  @Override
  public String toString() {
    return stateName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isAcceptingState ? 1231 : 1237);
    result = prime * result + ((outgoingTransitions == null) ? 0 : outgoingTransitions.hashCode());
    result = prime * result + ((stateName == null) ? 0 : stateName.hashCode());
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
    BFAutomatonState other = (BFAutomatonState) obj;
    if (isAcceptingState != other.isAcceptingState) {
      return false;
    }
    if (outgoingTransitions == null) {
      if (other.outgoingTransitions != null) {
        return false;
      }
    } else if (!outgoingTransitions.equals(other.outgoingTransitions)) {
      return false;
    }
    if (stateName == null) {
      if (other.stateName != null) {
        return false;
      }
    } else if (!stateName.equals(other.stateName)) {
      return false;
    }
    return true;
  }
}
