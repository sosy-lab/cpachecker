// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class AutomatonWithMetaData extends Automaton {

  private final Map<AutomatonTransition, GraphMLTransition> transitions;

  public AutomatonWithMetaData(
      String pName,
      Map<String, AutomatonVariable> pVars,
      List<AutomatonInternalState> pStates, String pInitialStateName,
      Map<AutomatonTransition, GraphMLTransition> pTransitions)
      throws InvalidAutomatonException {
    super(pName, pVars, pStates, pInitialStateName);
    transitions = ImmutableMap.copyOf(pTransitions);
  }

  public Map<AutomatonTransition, GraphMLTransition> getTransitions() {
    return transitions;
  }
}
