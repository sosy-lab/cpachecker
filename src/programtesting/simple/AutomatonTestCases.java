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
package programtesting.simple;

import programtesting.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cmdline.CPAMain;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.automaton.Automaton2;
import cpa.common.automaton.Label;
import cpa.common.automaton.NegationLabel;
import cpa.common.automaton.cfa.FunctionCallLabel;

/**
 * @author holzera
 *
 */
public class AutomatonTestCases {
  public static Automaton2<CFAEdge> getTestGoalAutomaton(CFAFunctionDefinitionNode pMainFunction) {
    Map<String, Automaton2<CFAEdge>> lMapping = new HashMap<String, Automaton2<CFAEdge>>();

    lMapping.put("001", getTestGoalAutomaton001(pMainFunction));
    lMapping.put("002", getTestGoalAutomaton002(pMainFunction));
    lMapping.put("003", getTestGoalAutomaton003(pMainFunction));

    Object lKey = CPAMain.cpaConfig.get("testing.testgoalautomaton");
    
    if (lMapping.containsKey(lKey)) {
      return lMapping.get(lKey);
    }

    assert(false);
    
    throw new RuntimeException("Invalid key for test goal automaton given!");
  }

  /* test cases */

  private static Automaton2<CFAEdge> getTestGoalAutomaton001(CFAFunctionDefinitionNode pMainFunction) {
    Automaton2<CFAEdge> lTestGoalAutomaton = new Automaton2<CFAEdge>();

    // label that matches the call to function special_case
    Label<CFAEdge> lSpecialCaseLabel = new FunctionCallLabel("special_case");

    int lInitialState = lTestGoalAutomaton.getInitialState();

    // as long as we do not see a call to special case we stay in the initial state
    lTestGoalAutomaton.addSelfLoop(lInitialState, new NegationLabel<CFAEdge>(lSpecialCaseLabel));

    int lState = lTestGoalAutomaton.getNewState();

    // this state is a test goal
    lTestGoalAutomaton.setFinal(lState);

    lTestGoalAutomaton.addTransition(lInitialState, lState, lSpecialCaseLabel);

    return lTestGoalAutomaton;
  }

  private static Automaton2<CFAEdge> getTestGoalAutomaton002(CFAFunctionDefinitionNode pMainFunction) {
    Label<CFAEdge> lSpecialCaseLabel = new FunctionCallLabel("special_case");
    
    LabelBasedPredicateGenerator lGenerator = new LabelBasedPredicateGenerator(lSpecialCaseLabel);
    
    Automaton2<CFAEdge> lTestGoalAutomaton = new Automaton2<CFAEdge>(lGenerator.getPredicateLabels(pMainFunction));
    
    // add self loop to initial state
    int lInitialState = lTestGoalAutomaton.getInitialState();
    
    // TODO make this deterministic
    lTestGoalAutomaton.addUnconditionalSelfLoop(lInitialState);
    
    return lTestGoalAutomaton;
  }
  
  private static Automaton2<CFAEdge> getTestGoalAutomaton003(CFAFunctionDefinitionNode pMainFunction) {
    PredicateGenerator lGenerator = new BasicBlockPredicateGenerator();
    
    Automaton2<CFAEdge> lTestGoalAutomaton = new Automaton2<CFAEdge>(lGenerator.getPredicateLabels(pMainFunction));
    
    // add self loop to initial state
    int lInitialState = lTestGoalAutomaton.getInitialState();

    // TODO make this deterministic
    lTestGoalAutomaton.addUnconditionalSelfLoop(lInitialState);

    System.out.println(lTestGoalAutomaton);
    
    return lTestGoalAutomaton;
  }
}
