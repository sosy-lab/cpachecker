/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpv.property;

import java.util.List;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;

/*
 * This property is represented by one or several automata.
 */
public final class AutomataSingleProperty extends AbstractSingleProperty {

  private final List<Automaton> automata;

  public AutomataSingleProperty(String pName, List<Automaton> pAutomata) {
    super(pName);
    automata = pAutomata;
  }

  @Override
  public void disableProperty() {
    for (Automaton automaton : automata) {
      automaton.disableAutomaton();
    }
  }

  @Override
  public void enableProperty() {
    for (Automaton automaton : automata) {
      automaton.enableAutomaton();
    }
  }

  @Override
  public boolean isTarget(AutomatonState pState) {
    if (automata.contains(pState.getOwningAutomaton())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void checkIfRelevant() {
    for (Automaton automaton : automata) {
      for (AutomatonInternalState state : automaton.getStates()) {
        for (AutomatonTransition transition : state.getTransitions()) {
          if (transition.isRelevant()) {
            relevant = true;
            return;
          }
        }
      }
    }
  }
}
