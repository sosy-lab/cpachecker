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
package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

/**
 * @author rhein
 */
public class Automaton {
  // default name of this automaton is "anonymous".
  private String name = "anonymous";
  /* The internal variables used by the actions/ assignments of this automaton.
   * This reference of the Map is unused because the actions/assignments get their reference from the parser.
   */
  private Map<String, AutomatonVariable> initVars;
  private List<AutomatonInternalState> states;
  private AutomatonInternalState initState;

  public Automaton(Map<String, AutomatonVariable> pVars, List<AutomatonInternalState> pStates,
      String pInit, LogManager pLogger) throws InvalidAutomatonException {
    this.initVars = pVars;
    this.states = pStates;
    for (AutomatonInternalState s : pStates) {
      if (s.getName().equals(pInit)) {
        this.initState = s;
      }
    }
    if (initState == null) {
      pLogger.log(Level.WARNING, "InitState not found. Automaton \"" + name + "\" is initiated with ErrorState");
      initState = AutomatonInternalState.ERROR;
    }
    // implicit error State (might be followState of Transitions)
    // pStates.add(AutomatonInternalState.ERROR);

    // set the FollowStates of all Transitions
    for (AutomatonInternalState s : pStates) {
      s.setFollowStates(pStates);
    }
  }

  public void setName(String pName) {
    this.name = pName;
  }
  public String getName() {
    return name;
  }

  AutomatonInternalState getInitialState() {
    return initState;
  }

  /**
   * Prints the contents of a DOT file representing this automaton to the PrintStream.
   * @param pOut
   */
  void writeDotFile(PrintStream pOut) {
    pOut.println("digraph " + name + "{");
    for (AutomatonInternalState s : states) {
      if (initState.equals(s)) {
        pOut.println(s.getStateId() + " [shape=\"circle\" color=\"green\" label=\"" +  s.getName() + "\"]");
      } else {
        pOut.println(s.getStateId() + " [shape=\"circle\" color=\"black\" label=\"" +  s.getName() + "\"]");
      }
      s.writeTransitionsToDotFile(pOut);
    }
    pOut.println("}");
  }

  public Map<String, AutomatonVariable> getInitialVariables() {
    return initVars;
  }

  /**
   * Assert this automaton fulfills the requirements of an ObserverAutomaton.
   * This means the Automaton does not modify other CPAs (Keyword MODIFY) and does not use the BOTTOM element (Keyword STOP).
   * @throws InvalidAutomatonException if the requirements are not fulfilled
   */
  public void assertObserverAutomaton() throws InvalidConfigurationException {
    for (AutomatonInternalState s : this.states) {
        for (AutomatonTransition t : s.getTransitions()) {
          if (!t.meetsObserverRequirements()) {
            throw new InvalidConfigurationException("The Transition " + t.toString() + "(" + s.toString() + ") is not valid for an ObserverAutomaton.");
          }
        }
      }
    }
}
