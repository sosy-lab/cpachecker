/**
 *
 */
package programtesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import compositeCPA.CompositeCPA;

import cpa.common.CPAAlgorithm;
import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.automaton.Label;
import cpa.common.automaton.NegationLabel;
import cpa.common.automaton.cfa.FunctionCallLabel;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabsCPA.SymbPredAbsCPA;
import cpa.testgoal.TestGoalCPA;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {

  public static Automaton<CFAEdge> getScopeRestrictionAutomaton() {
    // create simple scope restriction automaton that restricts nothing
    Automaton<CFAEdge> lScopeRestrictionAutomaton = new Automaton<CFAEdge>();
    Automaton<CFAEdge>.State lState = lScopeRestrictionAutomaton.getInitialState();
    lState.addUnconditionalSelfLoop();
    
    return lScopeRestrictionAutomaton;
  }
  
  public static Automaton<CFAEdge> getTestGoalAutomaton() {
    Automaton<CFAEdge> lTestGoalAutomaton = new Automaton<CFAEdge>();
    
    // label that matches the call to function special_case
    Label<CFAEdge> lSpecialCaseLabel = new FunctionCallLabel("special_case");
    
    Automaton<CFAEdge>.State lInitialState = lTestGoalAutomaton.getInitialState();
    
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
  
  public static Set<ArrayList<CFAEdge>> doIt (CFAFunctionDefinitionNode pMainFunction) {
    // create compositeCPA from automaton CPA and pred abstraction
    // TODO this must be a CPAPlus actually
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> ();
    try {
      cpas.add(new LocationCPA("sep", "sep"));
    } catch (CPAException e) {
      // for fixed values "sep", "sep" this is actually unreachable
      e.printStackTrace();
    }

    // get scope restriction automaton
    Automaton<CFAEdge> lScopeRestrictionAutomaton = getScopeRestrictionAutomaton();
    ScopeRestrictionCPA lScopeRestrictionCPA = new ScopeRestrictionCPA(lScopeRestrictionAutomaton);
    cpas.add(lScopeRestrictionCPA);

    // get test goal automaton
    Automaton<CFAEdge> lTestGoalAutomaton = getTestGoalAutomaton();
    TestGoalCPA lTestGoalCPA = new TestGoalCPA(lTestGoalAutomaton);
    cpas.add(lTestGoalCPA);

    // initialize symbolic predicate abstraction
    // TODO: include predicate abstraction
    /*SymbPredAbsCPA predAbsCPA = new SymbPredAbsCPA("sep", "sep");
    cpas.add(predAbsCPA);*/
    
    // create composite cpa
    ConfigurableProgramAnalysis cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
    CPAAlgorithm algo = new CPAAlgorithm();

    // every final state in the test goal automaton represents a 
    // test goal, so initialize test goals with the final states
    // of the test goal automaton
    Set<Automaton<CFAEdge>.State> lTestGoals = lTestGoalAutomaton.getFinalStates();

    // TODO: the resulting set of paths
    Set<ArrayList<CFAEdge>> lPaths = new HashSet<ArrayList<CFAEdge>>();
    
    // TODO: Remove this output
    System.out.println("#TestGoals = " + lTestGoals.size());
    
    while (!lTestGoals.isEmpty()) {
      // TODO: Simplify test goal automaton
      
      // testGoals to be passed in as precision
      AbstractElement lInitialElement = cpa.getInitialElement(pMainFunction);
      
      Collection<AbstractElement> lReachedElements = null;
      
      try {
        lReachedElements = algo.CPA(cpa, lInitialElement);
        
        // TODO: Remove this output
        for (AbstractElement lElement : lReachedElements) {
          System.out.println(lElement);
        }
      } catch (CPAException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
        
        // end test case generation
        break;
      }
      
      // TODO: Remove infeasible test goals from lTestGoals
      // These are the test goals in lTestGoals not occuring in lReachedElements
      // The infeasible test goals are the test goals that remained in the precision
      // we do not have to iterate through the set of reached elements
      //removeInfeasibleTestGoals(testGoals, reached);

      for (AbstractElement lElement : lReachedElements) {
        // are there any remaining test goals to be covered?
        if (lTestGoals.isEmpty()) {
          // we are done, every test goal is reached
          break;
        }
        
        if (cpa.getAbstractDomain().isBottomElement(lElement)) {
          continue;
        }
        
        CompositeElement lCompositeElement = (CompositeElement)lElement;
          
        AbstractElement lTmpElement = lCompositeElement.get(2);
        
        if (lTestGoalCPA.getAbstractDomain().isBottomElement(lTmpElement)) {
          // TODO: This should not happen
          continue;
        }

        // TODO: Why is there a isBottomElement but not a isTopElement?
        // is isBottomElement superfluous?
        if (lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTmpElement)) {
          // TODO: How to handle this, this element should never occur?
          // Should we consider it as covered every test goal?
          continue;
        }
        
        // now, we know it is an StateSetElement
        AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = (AutomatonCPADomain<CFAEdge>.StateSetElement)lTmpElement;
        
        final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
        
        // remove all covered test goals
        for (Automaton<CFAEdge>.State lState : lStates) {
          // is lState a remaining test goal?
          if (lState.isFinal() && lTestGoals.contains(lState)) {
            // TODO: check feasibility
            boolean isFeasible = true;
            
            if (isFeasible) {
              // remove the test goal from lTestGoals
              lTestGoals.remove(lState);
              
              // remove the test goal from the automaton
              lState.unsetFinal();
              
              // TODO: add path
              
              // TODO: Remove this output
              System.out.println("=> " + lElement.toString());
            }
            else {
              // TODO: Refine abstraction
            }
          }
        }
      }
      
      // TODO: invoke CBMC
      //runCBMC(paths);
    }
    
    return lPaths;
  }
}
