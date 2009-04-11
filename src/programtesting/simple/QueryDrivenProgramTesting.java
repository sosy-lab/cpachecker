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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import cmdline.CPAMain;
import common.Pair;
import compositeCPA.CompositePrecision;

import cpa.common.CPAAlgorithm;
import cpa.common.automaton.Automaton2;
import cpa.common.automaton.AutomatonCPADomain2;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.location.LocationCPA;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.UpdateablePredicateMap;
import cpa.symbpredabs.explicit.BDDMathsatExplicitAbstractManager;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpa.symbpredabs.explicit.ExplicitCPA;
import cpa.symbpredabs.explicit.ExplicitTransferRelation;
import cpa.testgoal.TestGoalCPA2;
import cpa.testgoal.TestGoalCPA2.TestGoalPrecision;
import exceptions.CPAException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;
import programtesting.simple.AcyclicPathProgramExtractor.AcyclicPathProgram;
import programtesting.simple.QDPTCompositeCPA.CFAEdgeEdge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
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

  public final static int mLocationCPAIndex = 0;
  public final static int mTestGoalCPAIndex = 1;
  public final static int mExplicitAbstractionCPAIndex = 2;
  
  public static void doIt(CFAMap pCfas, CFAFunctionDefinitionNode pMainFunction) {
    // create compositeCPA from automaton CPA and pred abstraction
    // TODO this must be a CPAPlus actually
    ArrayList<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis>();

    
    LocationCPA lLocationCPA = null;
    
    try {
      lLocationCPA = new LocationCPA("sep", "sep");
    }
    catch (Exception e) {
      e.printStackTrace();
      assert(false);
    }
    
    cpas.add(lLocationCPA);



    // get test goal automaton
    Automaton2<CFAEdge> lTestGoalAutomaton = AutomatonTestCases.getTestGoalAutomaton(pMainFunction);
    TestGoalCPA2 lTestGoalCPA = new TestGoalCPA2(lTestGoalAutomaton);
    cpas.add(lTestGoalCPA);
    
    Automaton2<CFAEdge> lOriginalTestGoalAutomaton = lTestGoalAutomaton;

    int lNumberOfOriginalTestGoals = lOriginalTestGoalAutomaton.getFinalStates().size();
    
    Set<Integer> lInfeasibleTestGoals = new HashSet<Integer>();



    // initialize symbolic predicate abstraction
    ExplicitCPA lExplicitAbstractionCPA = new ExplicitCPA("sep", "sep");
    cpas.add(lExplicitAbstractionCPA);

    ExplicitAbstractFormulaManager lEAFManager = lExplicitAbstractionCPA.getAbstractFormulaManager();
    SymbolicFormulaManager lSFManager = lExplicitAbstractionCPA.getFormulaManager();

    assert (lEAFManager instanceof BDDMathsatExplicitAbstractManager);

    BDDMathsatExplicitAbstractManager lMathsatManager = (BDDMathsatExplicitAbstractManager) lEAFManager;


    QDPTCompositeCPA cpa = new QDPTCompositeCPA(cpas, pMainFunction, lTestGoalCPA.getAbstractDomain(), mTestGoalCPAIndex);


    // initialize set of initial elements -- begin

    QDPTCompositeElement lFirstInitialElement = cpa.getInitialElement(pMainFunction);

    Comparator<QDPTCompositeElement> lDepthComparator = new Comparator<QDPTCompositeElement>() {

      @Override
      public int compare(QDPTCompositeElement pElement1, QDPTCompositeElement pElement2) {
        assert(pElement1 != null);
        assert(pElement2 != null);

        return (pElement1.getDepth() - pElement2.getDepth());
      }

    };

    Set<QDPTCompositeElement> lInitialElements = new TreeSet<QDPTCompositeElement>(lDepthComparator);

    lInitialElements.add(lFirstInitialElement);
    
    // initialize set of initial elements -- end


    int lLoopCounter = 0;

    while (lTestGoalAutomaton.hasFinalStates()) {
      // TODO remove this output
      System.out.println("NEXT LOOP (" + (lLoopCounter++) + ") #####################");


      lTestGoalAutomaton = lTestGoalAutomaton.getSimplifiedAutomaton();

      
      // print information about remaining test goals
      System.out.println("Number of remaining test goals: " + lTestGoalAutomaton.getFinalStates().size());

      OutputUtilities.printTestGoals2("Remaining Test Goals: ", lTestGoalAutomaton.getFinalStates());

      System.out.println(lNumberOfOriginalTestGoals + "/" + lTestGoalAutomaton.getFinalStates().size() + "/" + lTestGoalAutomaton.getNumberOfStates());
      

      // initialize precision
      CompositePrecision lInitialPrecision = cpa.getInitialPrecision(pMainFunction);
      TestGoalPrecision lTestGoalPrecision = (TestGoalPrecision)lInitialPrecision.get(mTestGoalCPAIndex);

      // TODO maybe we should just modify lOriginalTestGoalAutomaton
      TestGoalPrecision lNewTestGoalPrecision = lTestGoalPrecision.getIntersection(lTestGoalAutomaton.getReachableStates(lTestGoalAutomaton.getInitialState()));

      LinkedList<Precision> lPrecisions = new LinkedList<Precision>(lInitialPrecision.getPrecisions());

      lPrecisions.set(mTestGoalCPAIndex, lNewTestGoalPrecision);

      System.out.println(lNewTestGoalPrecision);

      CompositePrecision lNewInitialPrecision = new CompositePrecision(lPrecisions);


      if (lLoopCounter > 1) {
        System.out.println(lTestGoalAutomaton);

        //assert(false);

        cpa = new QDPTCompositeCPA(cpas, pMainFunction, lTestGoalCPA.getAbstractDomain(), mTestGoalCPAIndex);

        lFirstInitialElement = cpa.getInitialElement(pMainFunction);

        lInitialElements.clear();

        lInitialElements.add(lFirstInitialElement);

        TransferRelation lTransferRelation = lExplicitAbstractionCPA.getTransferRelation();

        ExplicitTransferRelation lExplicitTransferRelation = (ExplicitTransferRelation) lTransferRelation;

        lExplicitTransferRelation.clearART();
      }
      

      try {
        // perform cfa exploration
        CPAAlgorithm.CPAWithInitialSet(cpa, lInitialElements, lNewInitialPrecision, Collections.EMPTY_SET);
      } catch (CPAException e1) {
        e1.printStackTrace();
        assert(false);
      }
       
      assert(cpa.getTransferRelation().hasRoot());
      
      QDPTCompositeElement lRoot = cpa.getTransferRelation().getRoot();
  
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_a_", lRoot, lInitialElements);
      }
      
      
      
      Set<QDPTCompositeElement> lReachedElements = ARTUtilities.getReachedElements(lInitialElements);
      
      Comparator lMaxDepthComparator = new Comparator<QDPTCompositeElement>() {

        @Override
        public int compare(QDPTCompositeElement pElement1, QDPTCompositeElement pElement2) {
          assert(pElement1 != null);
          assert(pElement2 != null);

          return (pElement2.getMaxDepth() - pElement1.getMaxDepth());
        }

      };

      PriorityQueue<QDPTCompositeElement> lReachabilityTasks = new PriorityQueue<QDPTCompositeElement>(lReachedElements.size(), lMaxDepthComparator);
      
      lReachabilityTasks.addAll(lReachedElements);
      
      
            
      /*
       * TODO OPTIMIZATION if two art elements are really equivalent, we can 
       * skip the rechability check for one of them -> if one is contained in
       * another path we may be able to get the necessary information from the
       * longer reachability query.
       */
      
      int lNumberOfCallsToCBMCOld = CProver.getNumberOfCallsToCBMC();
      
      String lFunctionName = lRoot.getLocationNode().getFunctionName();

      int lTaskId = -1;

      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        Map<QDPTCompositeElement, String> lFormating = new HashMap<QDPTCompositeElement, String>();

        for (QDPTCompositeElement lElement : lReachabilityTasks) {
          lFormating.put(lElement, "style=filled, shape=ellipse, fillcolor=dimgrey, color=black");
        }

        OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_reachability_tasks_", lRoot, lFormating);
      }

      Set<QDPTCompositeElement> lInfeasibleElements = new HashSet<QDPTCompositeElement>();

      Map<QDPTCompositeElement, String> lReachabilityFormating = new HashMap<QDPTCompositeElement, String>();

      System.out.println(">>>>");

      for (QDPTCompositeElement lCurrentElement : lReachabilityTasks) {
        ExplicitAbstractElement lExplicitAbstractElement = lCurrentElement.projectTo(mExplicitAbstractionCPAIndex);

        System.out.println(lMathsatManager.toConcrete(lSFManager, lExplicitAbstractElement.getAbstraction()));
      }

      System.out.println("<<<<");

      while (!lReachabilityTasks.isEmpty()) {
        QDPTCompositeElement lCurrentElement = lReachabilityTasks.poll();

        // check whether lCurrentElement contains an unseen test goal -- begin
        AutomatonCPADomain2<CFAEdge>.StateSetElement lCurrentStateSetElement = lCurrentElement.projectTo(mTestGoalCPAIndex);

        boolean lContainsRemainingTestGoal = false;

        for (Integer lTestGoal : lTestGoalAutomaton.getFinalStates()) {
          Set<Integer> lCurrentTestGoals = lCurrentStateSetElement.getAcceptingStates();

          if (lCurrentTestGoals.contains(lTestGoal)) {
            lContainsRemainingTestGoal = true;

            break;
          }
        }
        // check whether lCurrentElement contains an unseen test goal -- end
        
        if (!lContainsRemainingTestGoal) {
          // lCurrentElement contains no unseen test goal, so, we skip it.
          continue;
        }

        lTaskId++;

        System.out.println("REACHABILITY TASK [" + (lTaskId + 1) + "]: " + lCurrentElement + "  (" + lCurrentElement.getDepth() + ")");

        // create a dag that represents the program to lCurrentElement
        
        AcyclicPathProgram lAcyclicPathProgram = AcyclicPathProgramExtractor.extract(lRoot, lCurrentElement, pCfas);
        
        Graph<QDPTCompositeElement, CFAEdgeEdge> lDAG = lAcyclicPathProgram.getDAG();

        Graph<BasicBlock, Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> lBasicBlocks = lAcyclicPathProgram.getBasicBlockGraph();

        if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
          OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_a_test_", lRoot, lDAG.getNodes());
          lBasicBlocks.printDotToFile("basicblocks_" + lLoopCounter + "_a_task" + lTaskId + "_");
        }


        // translate acyclic path program to C program
        String lProgram = AcyclicPathProgramTranslator.translate(lAcyclicPathProgram);

        System.out.println("Starting feasibility check ...");

        // check feasibility and get output of CBMC
        Pair<Boolean, String> lFeasibilityAndOutput = CProver.getFeasibilityAndOutput(lFunctionName, lProgram);
        
        if (lFeasibilityAndOutput.getFirst()) {
          // evaluate counter example to extract feasibility information
          
          List<CFAEdgeEdge> lFeasiblePath = AcyclicPathProgramTranslator.extractFeasiblePath(lFeasibilityAndOutput.getSecond(), lAcyclicPathProgram);

          assert(lFeasiblePath.size() > 0);

          QDPTCompositeElement lRootOfPath = lFeasiblePath.get(0).getParent();

          lReachabilityFormating.put(lRootOfPath, "shape=ellipse, color=black, fillcolor=greenyellow, style=filled");

          Set<Integer> lTestGoalsAlongPath = new HashSet<Integer>();

          AutomatonCPADomain2<CFAEdge>.StateSetElement lRootStateSetElement = lRootOfPath.projectTo(mTestGoalCPAIndex);

          lTestGoalsAlongPath.addAll(lRootStateSetElement.getAcceptingStates());

          for (CFAEdgeEdge lEdge : lFeasiblePath) {
            lReachabilityFormating.put(lEdge.getChild(), "shape=ellipse, color=black, fillcolor=greenyellow, style=filled");

            AutomatonCPADomain2<CFAEdge>.StateSetElement lStateSetElement = lEdge.getChild().projectTo(mTestGoalCPAIndex);

            lTestGoalsAlongPath.addAll(lStateSetElement.getAcceptingStates());
          }

          lTestGoalAutomaton.unsetFinal(lTestGoalsAlongPath);

          System.out.println(lTestGoalsAlongPath);
          
          System.out.println(true);
        }
        else {
          lInfeasibleElements.add(lCurrentElement);

          lReachabilityFormating.put(lCurrentElement, "shape=ellipse, color=black, fillcolor=orangered, style=filled");
          System.out.println(false);
        }
        
        if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
          OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_reachability_results_task_" + (lTaskId + 1) + "_", lRoot, lReachabilityFormating);
        }
      }

      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_reachability_results_", lRoot, lReachabilityFormating);
      }

      System.out.println("Remaining test goals: " + lTestGoalAutomaton.getFinalStates());


      System.out.println("Infeasible elements: " + lInfeasibleElements);


      lInitialElements.clear();


      // determine infeasible test goals -- begin

      Set<Integer> lRemainingTestGoals = new HashSet<Integer>();

      for (QDPTCompositeElement lInfeasibleElement : lInfeasibleElements) {
        AutomatonCPADomain2<CFAEdge>.StateSetElement lStateSetElement = lInfeasibleElement.projectTo(mTestGoalCPAIndex);

        lRemainingTestGoals.addAll(lStateSetElement.getAcceptingStates());
      }

      Set<Integer> lLocalInfeasibleTestGoals = new HashSet<Integer>(lTestGoalAutomaton.getFinalStates());

      lLocalInfeasibleTestGoals.removeAll(lRemainingTestGoals);

      lInfeasibleTestGoals.addAll(lLocalInfeasibleTestGoals);

      lTestGoalAutomaton.unsetFinal(lLocalInfeasibleTestGoals);

      // determine infeasible test goals -- end


      for (QDPTCompositeElement lInfeasibleElement : lInfeasibleElements) {
        AutomatonCPADomain2<CFAEdge>.StateSetElement lStateSetElement = lInfeasibleElement.projectTo(mTestGoalCPAIndex);

        Set<Integer> lStates = lStateSetElement.getStates();

        boolean lNeedsProcessing = false;

        for (Integer lState : lStates) {
          if (lTestGoalAutomaton.isFinalState(lState)) {
            lNeedsProcessing = true;

            break;
          }
        }

        if (lNeedsProcessing) {
          // element contains unreached test goals
          System.out.println("to be processed: " + lInfeasibleElement);

          ExplicitAbstractElement lExplicitAbstractElement = lInfeasibleElement.projectTo(mExplicitAbstractionCPAIndex);

          Deque<ExplicitAbstractElement> lExplicitAbstractionPath = getAbstractPath(lExplicitAbstractElement);

          System.out.println(lExplicitAbstractionPath);

          // TODO currently do not enable cpas.symbpredabs.shortestCexTrace
          Pair<CounterexampleTraceInfo, Integer> lPair = lMathsatManager.buildCounterexampleTrace2(lSFManager, lExplicitAbstractionPath);

          CounterexampleTraceInfo lInfo = lPair.getFirst();

          assert(lInfo.isSpurious());

          // copied from old cold
          // TODO check what this assertion means
          assert (lPair.getSecond().intValue() != -1);

          // lPair.getSecond().intValue() is the last feasible edge, but in
          // this context we should not need it.

          //System.out.println("Index: " + lPair.getSecond());



          // PREDICATE REFINEMENT

          // perform update of predicates -- begin
          
          UpdateablePredicateMap lUpdateablePredicateMap = (UpdateablePredicateMap) lExplicitAbstractionCPA.getPredicateMap();

          LinkedList<QDPTCompositeCPA.Edge> lPathToRoot = lInfeasibleElement.getPathToRoot();

          QDPTCompositeElement lFirstUpdatedElement = null;

          for (QDPTCompositeCPA.Edge lPathEdge : lPathToRoot) {
            QDPTCompositeElement lChild = lPathEdge.getChild();

            ExplicitAbstractElement lElement = lChild.projectTo(mExplicitAbstractionCPAIndex);

            Collection<Predicate> newpreds = lInfo.getPredicatesForRefinement(lElement);

            boolean lUpdated = lUpdateablePredicateMap.update(lElement.getLocation(), newpreds);

            if (lUpdated && lFirstUpdatedElement == null) {
              lFirstUpdatedElement = lChild;
            }
          }

          ExplicitAbstractElement lElement = lInfeasibleElement.projectTo(mExplicitAbstractionCPAIndex);

          Collection<Predicate> newpreds = lInfo.getPredicatesForRefinement(lElement);

          boolean lUpdated = lUpdateablePredicateMap.update(lElement.getLocation(), newpreds);

          if (lUpdated && lFirstUpdatedElement == null) {
            lFirstUpdatedElement = lInfeasibleElement;
          }

          // perform update of predicates -- end

          // lFirstUpdatedElement could be null if we process a prefix of an
          // already processed path
          if (lFirstUpdatedElement != null) {
            lInitialElements.add(lFirstUpdatedElement);
          }
        }
      }

      System.out.println("Calls to CBMC: " + (CProver.getNumberOfCallsToCBMC() - lNumberOfCallsToCBMCOld));
      
      
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_b_", lRoot, lInitialElements);
      }
    }
    
    OutputUtilities.printTestGoals2("Infeasible test goals (#" + lInfeasibleTestGoals.size() + ") = ", lInfeasibleTestGoals);
  }
}
