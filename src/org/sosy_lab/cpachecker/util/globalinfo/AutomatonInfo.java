// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.globalinfo;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;

public class AutomatonInfo {
  private final Map<Integer, AutomatonInternalState> idToState;
  private final Map<String, ControlAutomatonCPA> nameToCPA;

  AutomatonInfo() {
    idToState = new HashMap<>();
    nameToCPA = new HashMap<>();
  }

  public void register(Automaton automaton, ControlAutomatonCPA cpa) {
    for (AutomatonInternalState state : automaton.getStates()) {
      idToState.put(state.getStateId(), state);
    }
    nameToCPA.put(automaton.getName(), cpa);
  }

  public AutomatonInternalState getStateById(int id) {
    return idToState.get(id);
  }

  public ControlAutomatonCPA getCPAForAutomaton(String automatonName) {
    return nameToCPA.get(automatonName);
  }
}
