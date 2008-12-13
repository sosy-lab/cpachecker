/**
 *
 */
package programtesting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
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
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpa.symbpredabs.explicit.ExplicitCPA;
import cpa.symbpredabs.explicit.ExplicitTransferRelation;
import cpa.testgoal.TestGoalCPA;
import exceptions.CPAException;
import exceptions.RefinementNeededException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {
  
  public static class WrapperCPA implements ConfigurableProgramAnalysis {
    private CompositeCPA mCompositeCPA;
    
    public WrapperCPA(CompositeCPA pCompositeCPA) {
      mCompositeCPA = pCompositeCPA;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
      return mCompositeCPA.getAbstractDomain();
    }

    @Override
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
      return mCompositeCPA.getInitialElement(pNode);
    }

    @Override
    public MergeOperator getMergeOperator() {
      return mCompositeCPA.getMergeOperator();
    }

    @Override
    public StopOperator getStopOperator() {
      return mCompositeCPA.getStopOperator();
    }

    @Override
    public TransferRelation getTransferRelation() {
      return mCompositeCPA.getTransferRelation();
    }
    
    // TODO: Move newReachedSet into interface of ConfigurableProgramAnalysis and
    // provide an abstract ConfigurableProgramAnalysisImpl-Class that implements
    // it by default by creating a hash set?
    // TODO: During ART creation establish an order
    // that allows efficient querying for test goals
    public Collection<AbstractElement> newReachedSet() {
      
      
      return new HashSet<AbstractElement>();
    }
    
  }
  
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
  
  public static Deque<ExplicitAbstractElement> getAbstractPath(ExplicitAbstractElement pElement) {
    // TODO: Remove this output
    System.out.println("Abstract Path >>> BEGIN");
    
    ExplicitAbstractElement lPathElement = pElement;
    
    Deque<ExplicitAbstractElement> lPath = new LinkedList<ExplicitAbstractElement>();
    
    while (lPathElement != null) {
      // TODO: Remove this output
      System.out.println(lPathElement.toString());
      
      lPath.addFirst(lPathElement);
      
      lPathElement = lPathElement.getParent();
    }
    
    // TODO: Remove this output
    System.out.println("Abstract Path >>> BEGIN");    
    
    return lPath;
  }
  
  public static Set<Deque<ExplicitAbstractElement>> doIt (CFAFunctionDefinitionNode pMainFunction) {
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
    ExplicitCPA lExplicitAbstractionCPA = new ExplicitCPA("sep", "sep");
    cpas.add(lExplicitAbstractionCPA);
    
    ExplicitAbstractFormulaManager lEAFManager = lExplicitAbstractionCPA.getAbstractFormulaManager();
    SymbolicFormulaManager lSFManager = lExplicitAbstractionCPA.getFormulaManager();
    
    
    // create composite cpa
    CompositeCPA cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
       
    WrapperCPA lWrapperCPA = new WrapperCPA(cpa);
    
    CPAAlgorithm algo = new CPAAlgorithm();

    // every final state in the test goal automaton represents a 
    // test goal, so initialize test goals with the final states
    // of the test goal automaton
    Set<Automaton<CFAEdge>.State> lTestGoals = lTestGoalAutomaton.getFinalStates();

    // the resulting set of paths
    Set<Deque<ExplicitAbstractElement>> lPaths = new HashSet<Deque<ExplicitAbstractElement>>();
    
    while (!lTestGoals.isEmpty()) {
      // TODO: Simplify test goal automaton
      
      // TODO: testGoals to be passed in as precision
      AbstractElementWithLocation lInitialElement = lWrapperCPA.getInitialElement(pMainFunction);
      
      Collection<AbstractElementWithLocation> lReachedElements = null;
      
      try {
        lReachedElements = algo.CPA(lWrapperCPA, lInitialElement);
        
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

      // TODO: remove this
      boolean lSomethingCovered = false;
      
      for (AbstractElement lElement : lReachedElements) {
        // are there any remaining test goals to be covered?
        if (lTestGoals.isEmpty()) {
          // we are done, every test goal is reached
          break;
        }
        
        if (lWrapperCPA.getAbstractDomain().isBottomElement(lElement)) {
          continue;
        }
        
        CompositeElement lCompositeElement = (CompositeElement)lElement;
          
        AbstractElement lTmpElement = lCompositeElement.get(2);
        
        assert(!lTestGoalCPA.getAbstractDomain().isBottomElement(lTmpElement));
        
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
            lSomethingCovered = true;
            
            // TODO: Remove this output
            System.out.println("=> " + lElement.toString());
            
            Deque<ExplicitAbstractElement> lPath = getAbstractPath((ExplicitAbstractElement)lCompositeElement.get(3));
            
            CounterexampleTraceInfo lInfo = lEAFManager.buildCounterexampleTrace(lSFManager, lPath);
            
            if (lInfo.isSpurious()) {
              // TODO: Remove this output
              System.out.println("Path is infeasible");
              
              TransferRelation lTransferRelation = lExplicitAbstractionCPA.getTransferRelation();
              
              ExplicitTransferRelation lExplicitTransferRelation = (ExplicitTransferRelation)lTransferRelation;
              
              try {
                lExplicitTransferRelation.performRefinement(lPath, lInfo);
              }
              catch (RefinementNeededException e) {
                // TODO: Remove this output
                System.out.println("Refinement done!");
              }
              catch (Exception e) {
                e.printStackTrace();
                
                System.exit(1);
              }
            }
            else {
              // TODO: Remove this output
              System.out.println("Path is feasible");
              
              // remove the test goal from lTestGoals
              lTestGoals.remove(lState);
              
              // remove the test goal from the automaton
              lState.unsetFinal();
              
              // add feasible path to set of feasible paths
              lPaths.add(lPath);
            }
          }
          else {
            // TODO: Remove this output
            System.out.println("no");
          }
        }
      }
      
      // TODO: Remove this break as soon as infeasible test goals get removed
      // from lTestGoals (necessary condition for termination of while-loop)
      if (!lSomethingCovered) {
        break;
      }
      else {
        System.out.println("NEXT LOOP #####################");
      }
      
      // TODO: invoke CBMC
      //runCBMC(paths);
    }
    
    return lPaths;
  }
}
