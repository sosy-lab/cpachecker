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
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Lightweight form of {@link Automaton}, with the main difference that the transitions consists
 * only of {@link BooleanFormula}s
 */
public class BFAutomaton {

  private final String name;
  private final ImmutableList<BFAutomatonState> states;
  private final BFAutomatonState initState;

  public BFAutomaton(String pName, List<BFAutomatonState> pStates, String pInitialStateName)
      throws BFAutomatonException {
    name = checkNotNull(pName);
    states = ImmutableList.copyOf(pStates);
    checkNotNull(pInitialStateName);

    Map<String, BFAutomatonState> statesMap = Maps.newHashMapWithExpectedSize(pStates.size());
    for (BFAutomatonState state : pStates) {
      if (statesMap.put(state.getName(), state) != null) {
        throw new BFAutomatonException(
            "State " + state.getName() + " exists twice in automaton " + pName);
      }
    }

    initState = statesMap.get(pInitialStateName);
    if (initState == null) {
      throw new BFAutomatonException(
          "Inital state " + pInitialStateName + " not found in automaton " + pName);
    }

    // set the FollowStates of all Transitions
    for (BFAutomatonState state : pStates) {
      state.setFollowStates(statesMap);
    }
  }

  public ImmutableList<BFAutomatonState> getStates() {
    return states;
  }

  public String getName() {
    return name;
  }

  public BFAutomatonState getInitialState() {
    return initState;
  }

  ImmutableList<String> getAllAssumptions() {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
    for (BFAutomatonState state : states) {
      for (BFAutomatonTransition trans : state.getOutgoingTransitions()) {
        ImmutableList<BooleanFormula> assumptions = trans.getAssumptions();
        builder.addAll(
            assumptions.stream()
                .map(BooleanFormula::toString)
                .collect(ImmutableList.toImmutableList()));
      }
    }
    return builder.build().stream().distinct().collect(ImmutableList.toImmutableList());
  }

  @Override
  public String toString() {
    final StringBuilder str = new StringBuilder();

    str.append("BF AUTOMATON ").append(getName()).append("\n\n");

    str.append("INITIAL STATE ").append(initState).append(";\n\n");

    for (BFAutomatonState s : states) {
      str.append("STATE ").append(s.getName()).append(":\n");
      for (BFAutomatonTransition t : s.getOutgoingTransitions()) {
        str.append("  ").append(t);
        str.append(" GOTO ");
        str.append(t.getFollowState()).append(";\n");
      }
      str.append("\n");
    }

    str.append("END AUTOMATON\n");

    return str.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((states == null) ? 0 : states.hashCode());
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
    BFAutomaton other = (BFAutomaton) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (states == null) {
      if (other.states != null) {
        return false;
      }
    } else if (!states.equals(other.states)) {
      return false;
    }
    return true;
  }
}
