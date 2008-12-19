/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 * 
 */
package programtesting;

import java.util.HashMap;
import java.util.Map;

import cmdline.CPAMain;
import cfa.objectmodel.CFAEdge;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.Label;
import cpa.common.automaton.NegationLabel;
import cpa.common.automaton.cfa.FunctionCallLabel;

/**
 * @author holzera
 *
 */
public class AutomatonTestCases {
  public static Automaton<CFAEdge> getScopeRestrictionAutomaton() {
    Map<String, Automaton<CFAEdge>> lMapping =
                                               new HashMap<String, Automaton<CFAEdge>>();

    lMapping.put("001", getScopeRestrictionAutomaton001());

    Object lKey = CPAMain.cpaConfig.get("testing.scoperestrictionautomaton");
    
    if (lMapping.containsKey(lKey)) {
      return lMapping.get(lKey);
    }

    assert(false);
    
    throw new RuntimeException("Invalid key for scope restriction automaton given!");
  }

  public static Automaton<CFAEdge> getTestGoalAutomaton() {
    Map<String, Automaton<CFAEdge>> lMapping =
                                               new HashMap<String, Automaton<CFAEdge>>();

    lMapping.put("001", getTestGoalAutomaton001());

    Object lKey = CPAMain.cpaConfig.get("testing.testgoalautomaton");
    
    if (lMapping.containsKey(lKey)) {
      return lMapping.get(lKey);
    }

    assert(false);
    
    throw new RuntimeException("Invalid key for test goal automaton given!");
  }

  /* test cases */

  private static Automaton<CFAEdge> getScopeRestrictionAutomaton001() {
    // create simple scope restriction automaton that restricts nothing
    Automaton<CFAEdge> lScopeRestrictionAutomaton = new Automaton<CFAEdge>();
    Automaton<CFAEdge>.State lState =
                                      lScopeRestrictionAutomaton
                                          .getInitialState();
    lState.addUnconditionalSelfLoop();

    return lScopeRestrictionAutomaton;
  }

  private static Automaton<CFAEdge> getTestGoalAutomaton001() {
    Automaton<CFAEdge> lTestGoalAutomaton = new Automaton<CFAEdge>();

    // label that matches the call to function special_case
    Label<CFAEdge> lSpecialCaseLabel = new FunctionCallLabel("special_case");

    Automaton<CFAEdge>.State lInitialState =
                                             lTestGoalAutomaton
                                                 .getInitialState();

    // as long as we do not see a call to special case we stay in the initial state
    lInitialState.addSelfLoop(new NegationLabel<CFAEdge>(lSpecialCaseLabel));

    Automaton<CFAEdge>.State lState = lTestGoalAutomaton.createState();

    // we won't leave lState anymore once reached
    lState.addUnconditionalSelfLoop();

    // this state is a test goal
    lState.setFinal();

    lInitialState.addTransition(lSpecialCaseLabel, lState);

    return lTestGoalAutomaton;
  }
}
