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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.Pair;
import compositeCPA.CompositeElement;
import compositeCPA.CompositePrecision;

import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabs.explicit.BDDMathsatExplicitAbstractManager;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpa.symbpredabs.explicit.ExplicitCPA;
import cpa.symbpredabs.explicit.ExplicitTransferRelation;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {
  
  public static Deque<ExplicitAbstractElement> getAbstractPath(ExplicitAbstractElement pElement) {
    ExplicitAbstractElement lPathElement = pElement;
    
    Deque<ExplicitAbstractElement> lPath = new LinkedList<ExplicitAbstractElement>();
    
    while (lPathElement != null) {
      lPath.addFirst(lPathElement);
      
      lPathElement = lPathElement.getParent();
    }
    
    return lPath;
  }
  
  //private final static int mScopeRestrictionCPAIndex = 1;
  private final static int mTestGoalCPAIndex = 2;
  private final static int mAbstractionCPAIndex = 0;
  
  public static Set<Deque<ExplicitAbstractElement>> doIt (CFAMap pCfas, CFAFunctionDefinitionNode pMainFunction) {
    // create compositeCPA from automaton CPA and pred abstraction
    // TODO this must be a CPAPlus actually
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> ();

    // initialize symbolic predicate abstraction
    ExplicitCPA lExplicitAbstractionCPA = new ExplicitCPA("sep", "sep");
    cpas.add(lExplicitAbstractionCPA);
    
    ExplicitAbstractFormulaManager lEAFManager = lExplicitAbstractionCPA.getExplicitFormulaManager();

    // get scope restriction automaton
    Automaton<CFAEdge> lScopeRestrictionAutomaton = AutomatonTestCases.getScopeRestrictionAutomaton(pMainFunction);
    ScopeRestrictionCPA lScopeRestrictionCPA = new ScopeRestrictionCPA(lScopeRestrictionAutomaton);
    cpas.add(lScopeRestrictionCPA);
    
    // get test goal automaton
    Automaton<CFAEdge> lTestGoalAutomaton = AutomatonTestCases.getTestGoalAutomaton(pMainFunction);
    TestGoalCPA lTestGoalCPA = new TestGoalCPA(lTestGoalAutomaton);
    cpas.add(lTestGoalCPA);
    
//  FIXME This code was not adapted when the CPAAlgorithm class changed.
//  I commented it out to remove the compiler error. (Philipp)
    System.err.println("This is currently not working, fix the code.");
    System.exit(1);
//    CPAAlgorithm algo = new CPAAlgorithm();

    // every final state in the test goal automaton represents a 
    // test goal, so initialize test goals with the final states
    // of the test goal automaton
    Set<Automaton<CFAEdge>.State> lTestGoals = lTestGoalAutomaton.getFinalStates();

    // the resulting set of paths
    Set<Deque<ExplicitAbstractElement>> lPaths = new HashSet<Deque<ExplicitAbstractElement>>();
    
    FeasiblePathTree<ExplicitAbstractElement> lPathTree = new FeasiblePathTree<ExplicitAbstractElement>();
    
    while (!lTestGoals.isEmpty()) {
      // TODO remove this output
      System.out.println("NEXT LOOP #####################");
      
      cpas.remove(mTestGoalCPAIndex);
      
      Automaton<CFAEdge> lSimplifiedAutomaton = lTestGoalCPA.getAbstractDomain().getAutomaton().getSimplifiedAutomaton();
      lTestGoals = lSimplifiedAutomaton.getFinalStates();
      
      // TODO remove this output
      //printTestGoals("Remaining Test Goals: ", lTestGoals);
      System.out.println("Number of remaining test goals: " + lTestGoals.size());
      
      // TODO since lTestCPA is added as last CPA, the remove above should remove the last -> assure index consistency
      lTestGoalCPA = new TestGoalCPA(lSimplifiedAutomaton);
      cpas.add(lTestGoalCPA);
      
      // create composite cpa
      //CompositeCPA cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
      QDPTCompositeCPA cpa = new QDPTCompositeCPA(cpas, pMainFunction,
          lTestGoalCPA.getAbstractDomain(), mTestGoalCPAIndex);
      
      
      AbstractElement lInitialElement = cpa.getInitialElement(pMainFunction);
      Precision lInitialPrecision = cpa.getInitialPrecision(pMainFunction);
      
      
      // TODO This is kind of a hack
      CompositePrecision lCompositePrecision = (CompositePrecision)lInitialPrecision;
      TestGoalPrecision lTestGoalPrecision = (TestGoalPrecision)lCompositePrecision.get(mTestGoalCPAIndex);
      // reset precision to test goals
      // TODO Hack
      lTestGoalPrecision.setTestGoals(lTestGoals);
      
      
      // commented out by Philipp, too
//      try {
//         algo.CPA(cpa, lInitialElement, lInitialPrecision);
//      } catch (CPAException e1) {
//        CPAMain.logManager.logException(Level.WARNING, e1, "");
//        
//        // end test case generation
//        break;
//      }
      
      
      // TODO Remove this output
      printTestGoals("Infeasible Test Goals: ", lTestGoalPrecision.getRemainingFinalStates());
      
      // Remove the infeasible test goals. If the set of remaining final states is
      // not empty this means that we have fully traversed an overapproximation
      // of the reachable state space. This shows that the remaing goals are not
      // reachable at all.
      lTestGoals.removeAll(lTestGoalPrecision.getRemainingFinalStates());
      
      
      Set<Pair<Deque<ExplicitAbstractElement>, CounterexampleTraceInfo>> lInfeasiblePaths = new HashSet<Pair<Deque<ExplicitAbstractElement>, CounterexampleTraceInfo>>();
      
      
      // process abstract reachability tree
      ParametricAbstractReachabilityTree<CompositeElement> lAbstractReachabilityTree = cpa.getTransferRelation().getAbstractReachabilityTree();
      
      assert(lAbstractReachabilityTree.hasRoot());
      
      CompositeElement lRoot = lAbstractReachabilityTree.getRoot();
      
      Stack<Pair<CompositeElement, Iterator<CompositeElement>>> lStack = new Stack<Pair<CompositeElement, Iterator<CompositeElement>>>();
      
      assert(!lAbstractReachabilityTree.getChildren(lRoot).isEmpty());
      
      lStack.push(new Pair(lRoot, lAbstractReachabilityTree.getChildren(lRoot).iterator()));
      
      
      while (!lStack.empty() && !lTestGoals.isEmpty()) {
        Pair<CompositeElement, Iterator<CompositeElement>> lCurrentPair = lStack.peek();

        Iterator<CompositeElement> lIterator = lCurrentPair.getSecond();

        if (lIterator.hasNext()) {
          CompositeElement lChild = lIterator.next();

          Collection<CompositeElement> lGrandchildren = lAbstractReachabilityTree.getChildren(lChild);

          if (lGrandchildren.isEmpty()) {
            // we are at a leave

            AbstractElement lTmpElement = lChild.get(mTestGoalCPAIndex);

            // the ART should not contain the bottom element
            assert (!lTestGoalCPA.getAbstractDomain().getBottomElement().equals(lTmpElement));

            // top element should never occur
            assert (!lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTmpElement));

            // now, we know it is an StateSetElement
            AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lTmpElement);

            final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();

            boolean lHasUncoveredTestGoal = false;

            for (Automaton<CFAEdge>.State lState : lStates) {
              if (lState.isFinal() && lTestGoals.contains(lState)) {
                lHasUncoveredTestGoal = true;
                break;
              }
            }

            if (lHasUncoveredTestGoal) {
              // check feasibility
              Deque<ExplicitAbstractElement> lPath = getAbstractPath((ExplicitAbstractElement) lChild.get(mAbstractionCPAIndex));

              assert (lEAFManager instanceof BDDMathsatExplicitAbstractManager);

              BDDMathsatExplicitAbstractManager lMathsatManager = (BDDMathsatExplicitAbstractManager) lEAFManager;

              Pair<CounterexampleTraceInfo, Integer> lPair = lMathsatManager.buildCounterexampleTrace2(lPath);

              CounterexampleTraceInfo lInfo = lPair.getFirst();

              if (lInfo.isSpurious()) {
                // TODO: Remove this output
                System.out.println("Path is infeasible");

                assert (lPair.getSecond().intValue() != -1);

                //System.out.println("Index: " + lPair.getSecond() + ", " + lPath.size() + ", " + lStack.size());

                lInfeasiblePaths.add(new Pair<Deque<ExplicitAbstractElement>, CounterexampleTraceInfo>(lPath, lInfo));

                // backtrack
                while (lStack.size() >= lPair.getSecond().intValue() - 1) {
                  lStack.pop();
                }

                //System.out.println("performed backtracking");

                // TODO check correctness of the following approach

                CompositeElement lBacktrackElement = lStack.peek().getFirst();

                Deque<ExplicitAbstractElement> lBacktrackPath = getAbstractPath((ExplicitAbstractElement) lBacktrackElement.get(mAbstractionCPAIndex));

                // TODO remove this correctness check in production code
                Pair<CounterexampleTraceInfo, Integer> lBacktrackPair = lMathsatManager.buildCounterexampleTrace2(lBacktrackPath);

                CounterexampleTraceInfo lBacktrackInfo = lBacktrackPair.getFirst();

                assert (!lBacktrackInfo.isSpurious());


                AbstractElement lBacktrackTmpElement = lChild.get(mTestGoalCPAIndex);

                // the ART should not contain the bottom element
                // TODO move this out of feasibilty check to stack insertion
                assert (!lTestGoalCPA.getAbstractDomain().getBottomElement().equals(lBacktrackTmpElement));

                // top element should never occur
                // TODO move this out of feasibilty check to stack insertion
                assert (!lTestGoalCPA.getAbstractDomain().getTopElement().equals(lBacktrackTmpElement));

                // now, we know it is an StateSetElement
                AutomatonCPADomain<CFAEdge>.StateSetElement lBacktrackTestGoalCPAElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lBacktrackTmpElement);

                final Set<Automaton<CFAEdge>.State> lBacktrackStates = lBacktrackTestGoalCPAElement.getStates();

                // remove the test goal from lTestGoals
                lTestGoals.removeAll(lBacktrackStates);

                // remove the test goal from the automaton
                for (Automaton<CFAEdge>.State lState : lBacktrackStates) {
                  lState.unsetFinal();
                }

                // add feasible path to set of feasible paths
                lPaths.add(lBacktrackPath);
                lPathTree.addPath(lBacktrackPath);
              } else {
                // TODO: Remove this output
                System.out.println("Path is feasible");

                // remove the test goal from lTestGoals
                lTestGoals.removeAll(lStates);

                // remove the test goal from the automaton
                for (Automaton<CFAEdge>.State lState : lStates) {
                  lState.unsetFinal();
                }

                // add feasible path to set of feasible paths
                lPaths.add(lPath);
                lPathTree.addPath(lPath);
              }
            }
          } else {
            lStack.push(new Pair<CompositeElement, Iterator<CompositeElement>>(lChild, lAbstractReachabilityTree.getChildren(lChild).iterator()));
          }
        } else {
          // we are done with all children
          lStack.pop();
        }
      }
      
      // do abstraction refinement
      if (!lInfeasiblePaths.isEmpty()) {
        // TODO: Remove this output
        System.out.println("Number of infeasible paths: " + lInfeasiblePaths.size());

        UpdateablePredicateMap lUpdateablePredicateMap = (UpdateablePredicateMap) lExplicitAbstractionCPA.getPredicateMap();

        boolean lHasUpdatedPredicates = false;

        for (Pair<Deque<ExplicitAbstractElement>, CounterexampleTraceInfo> lPair : lInfeasiblePaths) {
          for (ExplicitAbstractElement e : lPair.getFirst()) {
            Collection<Predicate> newpreds = lPair.getSecond().getPredicatesForRefinement(e);

            boolean lBoolean = lUpdateablePredicateMap.update(e.getLocation(), newpreds);

            lHasUpdatedPredicates |= lBoolean;
          }
        }

        // ensure some progress in refinement
        assert (lHasUpdatedPredicates);

        TransferRelation lTransferRelation = lExplicitAbstractionCPA.getTransferRelation();

        ExplicitTransferRelation lExplicitTransferRelation = (ExplicitTransferRelation) lTransferRelation;

        lExplicitTransferRelation.clearART();

        // TODO: Remove this output
        System.out.println("Refinement done!");
      }
    }
    
    System.out.println("FEASIBLE PATHS");
    
    System.out.println("lPaths: " + lPaths.size());
    
    System.out.println("lPathTree: " + lPathTree.getMaximalPaths().size());
    
    Map<Deque<ExplicitAbstractElement>, List<String>> lTranslations = AbstractPathToCTranslator.translatePaths(pCfas, lPaths);

    /*for (Entry<Deque<ExplicitAbstractElement>, Boolean> lVerified : CProver.checkSat(lTranslations).entrySet()) {
      if (lVerified.getValue()) {
        // test goal still not matched
        // true means the path is not feasible since assert(false) at the end of the C program
        // is not reachable
        // TODO implement handling of this case
        
        assert(false);
      }
      else {
        // test goal matched
        // false means the path is feasible since assert(false) at the end of the C program
        // is reachable
        
      }
    }*/
    
    for (Entry<Deque<ExplicitAbstractElement>, List<String>> lTranslation : lTranslations.entrySet()) {
      String lFunctionName = lTranslation.getKey().getFirst().getLocationNode().getFunctionName(); 
      
      FShell.isFeasible(lTranslation.getValue(), lFunctionName + "_0");
    }
    
    System.out.println("#Test cases computed: " + lPaths.size());
    
    return lPaths;
  }
  
  public static void printTestGoals(String pTitle, Collection<Automaton<CFAEdge>.State> pTestGoals) {
    System.out.print(pTitle);

    printTestGoals(pTestGoals);
  }
  
  public static void printTestGoals(Collection<Automaton<CFAEdge>.State> pTestGoals) {
    boolean lFirstTestGoal = true;

    System.out.print("[");

    for (Automaton<CFAEdge>.State lTestGoal : pTestGoals) {
      if (lFirstTestGoal) {
        lFirstTestGoal = false;
      } else {
        System.out.print(",");
      }

      System.out.print("q" + lTestGoal.getIndex());
    }

    System.out.println("]");
  }
}
