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
