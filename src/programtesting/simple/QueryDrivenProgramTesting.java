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

import programtesting.summary.*;
import programtesting.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import cmdline.CPAMain;
import compositeCPA.CompositePrecision;

import cpa.common.CPAAlgorithm;
import cpa.common.CallElement;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;
import exceptions.CPAException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import programtesting.simple.QDPTCompositeCPA.Edge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {
  
  public final static int mLocationCPAIndex = 0;
  public final static int mScopeRestrictionCPAIndex = 1;
  public final static int mTestGoalCPAIndex = 2;
  //public final static int mAbstractionCPAIndex = 1;
  
  public static Set<Deque<SymbPredAbsAbstractElement>> doIt (CFAMap pCfas, CFAFunctionDefinitionNode pMainFunction) {
    System.out.println("SummaryCPA based Test Case Generation.");
    
    // create compositeCPA from automaton CPA and pred abstraction
    // TODO this must be a CPAPlus actually
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> ();

    
    LocationCPA lLocationCPA = null;
    
    try {
      lLocationCPA = new LocationCPA("sep", "sep");
    }
    catch (Exception e) {
      e.printStackTrace();
      assert(false);
    }
    
    cpas.add(lLocationCPA);
    
    // get scope restriction automaton
    Automaton<CFAEdge> lScopeRestrictionAutomaton = AutomatonTestCases.getScopeRestrictionAutomaton(pMainFunction);
    ScopeRestrictionCPA lScopeRestrictionCPA = new ScopeRestrictionCPA(lScopeRestrictionAutomaton);
    cpas.add(lScopeRestrictionCPA);
    
    // get test goal automaton
    Automaton<CFAEdge> lTestGoalAutomaton = AutomatonTestCases.getTestGoalAutomaton(pMainFunction);
    TestGoalCPA lTestGoalCPA = new TestGoalCPA(lTestGoalAutomaton);
    cpas.add(lTestGoalCPA);
      
    QDPTCompositeCPA cpa = new QDPTCompositeCPA(cpas, pMainFunction, lTestGoalCPA.getAbstractDomain(), mTestGoalCPAIndex);
    
    
    
    CPAAlgorithm algo = new CPAAlgorithm();

    
    
    // every final state in the test goal automaton represents a 
    // test goal, so initialize test goals with the final states
    // of the test goal automaton
    Set<Automaton<CFAEdge>.State> lTestGoals = new HashSet<Automaton<CFAEdge>.State>(lTestGoalAutomaton.getFinalStates());
    int lNumberOfOriginalTestGoals = lTestGoals.size();
    
    Set<Automaton<CFAEdge>.State> lInfeasibleTestGoals = new HashSet<Automaton<CFAEdge>.State>();

    
    int lLoopCounter = 0;
          
        
    // calculate initial edges
    QDPTCompositeElement lFirstInitialElement = cpa.getInitialElement(pMainFunction);
    
    HashMap<QDPTCompositeElement, Set<CFAEdge>> lInitialElementsMap = new HashMap<QDPTCompositeElement, Set<CFAEdge>>();

    Set<CFAEdge> lOutgoingEdges = ARTUtilities.getOutgoingCFAEdges(lFirstInitialElement);

    // we can rule out exit
    if (lOutgoingEdges.size() > 0) {
      lInitialElementsMap.put(lFirstInitialElement, lOutgoingEdges);
    }
        

    // the resulting set of paths
    Set<List<AbstractElementWithLocation>> lPaths = new HashSet<List<AbstractElementWithLocation>>();
    
    FeasiblePathTree<AbstractElementWithLocation> lPathTree = new FeasiblePathTree<AbstractElementWithLocation>();
    
    Translator lTranslator = new Translator(pCfas);
    
    while (!lTestGoals.isEmpty()) {
      // TODO remove this output
      System.out.println("NEXT LOOP (" + (lLoopCounter++) + ") #####################");
            
      // print information about remaining test goals
      System.out.println("Number of remaining test goals: " + lTestGoals.size());
      
      OutputUtilities.printTestGoals("Remaining Test Goals: ", lTestGoals);
      
      
      System.out.println(lNumberOfOriginalTestGoals + "/" + lTestGoalAutomaton.getFinalStates().size() + "/" + lTestGoalAutomaton.getNumberOfStates());
      
      assert(lNumberOfOriginalTestGoals == lTestGoalAutomaton.getFinalStates().size());
      
      
      // initialize precision
      Precision lInitialPrecision = cpa.getInitialPrecision(pMainFunction);
      
      // TODO This is kind of a hack
      CompositePrecision lCompositePrecision = (CompositePrecision)lInitialPrecision;
      TestGoalPrecision lTestGoalPrecision = (TestGoalPrecision)lCompositePrecision.get(mTestGoalCPAIndex);
      // reset precision to test goals
      // TODO Hack
      lTestGoalPrecision.setTestGoals(lTestGoals);

      
      HashMap<QDPTCompositeElement, Set<CFAEdge>> lOldInitialElementsMap = new HashMap<QDPTCompositeElement, Set<CFAEdge>>(lInitialElementsMap);
      
      Set<Edge> lInitialEdges = new HashSet<Edge>();
      
      try {
        // create list of initial edges and initial states
        Set<QDPTCompositeElement> lInitialElements = new HashSet<QDPTCompositeElement>();
        
        for (Entry<QDPTCompositeElement, Set<CFAEdge>> lEntry : lInitialElementsMap.entrySet()) {
          QDPTCompositeElement lCurrentElement = lEntry.getKey();
          
          for (CFAEdge lCFAEdge : lEntry.getValue()) {
            try {
              AbstractElement lSuccessor = cpa.getTransferRelation().getAbstractSuccessor(lCurrentElement, lCFAEdge, lInitialPrecision);
              
              // NOTE: bottom can be produced because of not matching call stacks
              if (!cpa.getAbstractDomain().isBottomElement(lSuccessor)) {
                assert(lSuccessor instanceof QDPTCompositeElement);
              
                lInitialElements.add((QDPTCompositeElement)lSuccessor);
                lInitialEdges.add(((QDPTCompositeElement)lSuccessor).getEdgeToParent());
              }
            }
            catch (Exception e) {
              e.printStackTrace();
              assert(false);
            }
          }
        }
                
        // perform cfa exploration
        algo.CPAWithInitialSet(cpa, lInitialElements, lInitialPrecision, ARTUtilities.getReachedElements(lInitialElementsMap));
        
        lInitialElementsMap.clear();
      } catch (CPAException e1) {
        e1.printStackTrace();
        assert(false);
      }
       
      assert(cpa.getTransferRelation().hasRoot());
      
      QDPTCompositeElement lRoot = cpa.getTransferRelation().getRoot();
  
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_a_", lRoot, lOldInitialElementsMap.keySet());
      }
      
      LinkedList<Edge> lWorklist = new LinkedList<Edge>(lInitialEdges);
            
      int lPathCounter = 0;
      int lPathMaxLength = 0;
      
      int lAllCallsToCBMC = 0;
      
      while (!lWorklist.isEmpty() && !lTestGoals.isEmpty()) {
        Edge lCurrentEdge = lWorklist.removeFirst();
        
        QDPTCompositeElement lCurrentElement = lCurrentEdge.getChild();
        
        if (lCurrentElement.hasChildren()) {
          // we are at an intermediate node
          
          for (Edge lEdge : lCurrentElement.getChildren()) {
            lWorklist.addFirst(lEdge);
          }
        }
        else {
          // we are at a leaf
          
          lPathCounter++;
         
          int lDepth = lCurrentElement.getDepth();
          
          if (lDepth > lPathMaxLength) {
            lPathMaxLength = lDepth;
          }
          
          // check feasibility
          List<Edge> lPathToRoot = lCurrentElement.getPathToRoot();
          
          boolean lFeasible = false;

          Edge lInfeasibilityCause = null;

          int lCallsToCBMCCounter = 0;
          
          QDPTCompositeElement lLastFeasibleElement = lCurrentElement;
          
          HashSet<Edge> lBacktrackingSet = new HashSet<Edge>();
          
          do {
            String lPathCSource = lTranslator.translate(lPathToRoot);
            
            lCallsToCBMCCounter++;
            lFeasible = CProver.isFeasible(lRoot.getLocationNode().getFunctionName(), lPathCSource);

            if (!lFeasible) {
              // what's about function pointers?
              int lRemoveIndex = lPathToRoot.size() - 1;
              Edge lRemoveEdgeTmp = lPathToRoot.get(lPathToRoot.size() - 1);
              
              if (lInfeasibilityCause != null) {
                for (Edge lChildEdge : lInfeasibilityCause.getParent().getChildren()) {
                  lBacktrackingSet.add(lChildEdge);
                }
              }
              
              assert(lRemoveEdgeTmp instanceof QDPTCompositeCPA.CFAEdgeEdge);
              
              QDPTCompositeCPA.CFAEdgeEdge lRemoveEdge = (QDPTCompositeCPA.CFAEdgeEdge)lRemoveEdgeTmp;
              
              while (lRemoveEdge.getCFAEdge().getEdgeType() != CFAEdgeType.AssumeEdge) {
                lPathToRoot.remove(lRemoveIndex);
                
                // If parent has not more than one child then this edge is no
                // longer in the worklist because it was processed already.
                if (lRemoveEdge.getParent().getNumberOfChildren() > 1) {
                  for (Edge lChildEdge : lRemoveEdge.getParent().getChildren()) {
                    lBacktrackingSet.add(lChildEdge);
                  }
                }
                
                lRemoveIndex--;
                lRemoveEdgeTmp = lPathToRoot.get(lRemoveIndex);
                
                assert(lRemoveEdgeTmp instanceof QDPTCompositeCPA.CFAEdgeEdge);
              
                lRemoveEdge = (QDPTCompositeCPA.CFAEdgeEdge)lRemoveEdgeTmp;
              }
              
              lPathCSource = lTranslator.translate(lPathToRoot);
              
              // TODO remove this from production code -> lTmpFeasible stuff
              //lCallsToCBMCCounter++;
              boolean lTmpFeasible = CProver.isFeasible(lRoot.getLocationNode().getFunctionName(), lPathCSource);

              assert (!lTmpFeasible);

              // remove assume edge
              lInfeasibilityCause = lPathToRoot.remove(lPathToRoot.size() - 1);
              
              lLastFeasibleElement = lInfeasibilityCause.getParent();
            }
          } while (!lFeasible);
          
          lAllCallsToCBMC += lCallsToCBMCCounter;
          
          // backtrack
          lWorklist.removeAll(lBacktrackingSet);
          
          
          // remove covered test goals
          AbstractElement lTestGoalElement = lLastFeasibleElement.get(mTestGoalCPAIndex);

          // the ART should not contain the bottom element
          // TODO move this out of feasibilty check to stack insertion
          assert (!lTestGoalCPA.getAbstractDomain().isBottomElement(lTestGoalElement));

          // top element should never occur
          // TODO move this out of feasibilty check to stack insertion
          assert (!lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTestGoalElement));

          // now, we know it is an StateSetElement
          AutomatonCPADomain<CFAEdge>.StateSetElement lStateSetElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lTestGoalElement);

          final Set<Automaton<CFAEdge>.State> lStates = lStateSetElement.getStates();

          // remove the test goal from lTestGoals
          lTestGoals.removeAll(lStates);

          
          // reachable leaf node
          if (lInfeasibilityCause == null) {
            Set<CFAEdge> lTmpOutgoingEdges = ARTUtilities.getOutgoingCFAEdges(lCurrentElement);

            // we can rule out exit
            if (lTmpOutgoingEdges.size() > 0) {
              lInitialElementsMap.put(lCurrentElement, lTmpOutgoingEdges);
            }
          }
          else {
            lInfeasibilityCause.getChild().remove();
          }
        }
      }
      
      System.out.println("Calls to CBMC: " + lAllCallsToCBMC);
      
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_b_", lRoot, lInitialElementsMap.keySet());
      }
      
      if (lInitialElementsMap.isEmpty()) {
        // we have nothing to explore anymore, so all remaining test goals
        // are infeasible
        lInfeasibleTestGoals.addAll(lTestGoals);
        lTestGoals.clear();
      }
      else {
        if (!lTestGoals.isEmpty()) {
          System.out.println(lInitialElementsMap);

          ARTUtilities.mergePaths(cpa, lTestGoalCPA, lRoot, lInitialElementsMap);
          
          System.out.println(lInitialElementsMap);
          
          
          HashSet<QDPTCompositeElement> lPropagateableInitialElements = new HashSet<QDPTCompositeElement>();
          
          for (QDPTCompositeElement lInitialElement : lInitialElementsMap.keySet()) {
            if (ARTUtilities.isPropagateable(lInitialElement)) {
              lPropagateableInitialElements.add(lInitialElement);
            }
          }
          
          for (QDPTCompositeElement lInitialElement : lPropagateableInitialElements) {
            ARTUtilities.propagate(lInitialElement, lInitialElementsMap);
          }
          
          System.out.println(lInitialElementsMap);
                    
          
          if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
            OutputUtilities.outputAbstractReachabilityTree("art_" + lLoopCounter + "_c_", lRoot, lInitialElementsMap.keySet());
          }
        }
      }
      
      System.out.println();
      System.out.println("lWorklist.isEmpty() = " + lWorklist.isEmpty());
      System.out.println("lTestGoals.isEmpty() = " + lTestGoals.isEmpty());
      System.out.println("lPathCounter = " + lPathCounter);
      System.out.println("lPathMaxLength = " + lPathMaxLength);
    }
    
    System.out.println("FEASIBLE PATHS");
    System.out.println("lPaths: " + lPaths.size());
    System.out.println("lPathTree: " + lPathTree.getMaximalPaths().size());
    
    OutputUtilities.printTestGoals("Infeasible test goals (#" + lInfeasibleTestGoals.size() + ") = ", lInfeasibleTestGoals);
   
    for (List<AbstractElementWithLocation> p : lPaths) {
      List<String> strpath = AbstractPathToCTranslator.translatePath(pCfas, AbstractPathToCTranslator.getPath(p, null));
      String lFunctionName = p.get(0).getLocationNode().getFunctionName(); 
      //assert(CProver.isFeasible(p.get(0).getLocationNode().getFunctionName(), strpath));
      FShell.isFeasible(strpath, lFunctionName + "_0");
    }
    
    System.out.println("#Test cases computed: " + lPaths.size());
    
    return null;
  }
}
