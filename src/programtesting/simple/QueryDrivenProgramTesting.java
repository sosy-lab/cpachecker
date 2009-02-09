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
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import cfa.objectmodel.CFANode;
import cmdline.CPAMain;
import compositeCPA.CompositePrecision;

import cpa.common.CPAAlgorithm;
import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.location.LocationCPA;
import cpa.location.LocationElement;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;
import exceptions.CPAException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;
import programtesting.simple.QDPTCompositeCPA.Edge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {
  
  private final static int mLocationCPAIndex = 0;
  private final static int mScopeRestrictionCPAIndex = 1;
  private final static int mTestGoalCPAIndex = 2;
  //private final static int mAbstractionCPAIndex = 1;
  
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

    Set<CFAEdge> lOutgoingEdges = getOutgoingCFAEdges(lFirstInitialElement);

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
      
      printTestGoals("Remaining Test Goals: ", lTestGoals);
      
      
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
              
              // hack
              assert(lSuccessor instanceof QDPTCompositeElement);
              
              if (!cpa.getAbstractDomain().isBottomElement(lSuccessor)) {
                lInitialElements.add((QDPTCompositeElement)lSuccessor);
                lInitialEdges.add(((QDPTCompositeElement)lSuccessor).getEdgeToParent());
              }
              else {
                // TODO remove assert(false)
                assert(false);
              }
            }
            catch (Exception e) {
              e.printStackTrace();
              assert(false);
            }
          }
        }
                
        // perform cfa exploration
        algo.CPAWithInitialSet(cpa, lInitialElements, lInitialPrecision);
        
        lInitialElementsMap.clear();
      } catch (CPAException e1) {
        e1.printStackTrace();
        assert(false);
      }
       
      assert(cpa.getTransferRelation().hasRoot());
      
      QDPTCompositeElement lRoot = cpa.getTransferRelation().getRoot();
  
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        outputAbstractReachabilityTree("art_" + lLoopCounter + "_a_", lRoot, lOldInitialElementsMap.keySet());
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
            Set<CFAEdge> lTmpOutgoingEdges = getOutgoingCFAEdges(lCurrentElement);

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
        outputAbstractReachabilityTree("art_" + lLoopCounter + "_b_", lRoot, lInitialElementsMap.keySet());
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
          
          mergePaths(cpa, lTestGoalCPA, lRoot, lInitialElementsMap);
          
          System.out.println(lInitialElementsMap);
          
          
          HashSet<QDPTCompositeElement> lPropagateableInitialElements = new HashSet<QDPTCompositeElement>();
          
          for (QDPTCompositeElement lInitialElement : lInitialElementsMap.keySet()) {
            if (isPropagateable(lInitialElement)) {
              lPropagateableInitialElements.add(lInitialElement);
            }
          }
          
          for (QDPTCompositeElement lInitialElement : lPropagateableInitialElements) {
            propagate(lInitialElement, lInitialElementsMap);
          }
          
          System.out.println(lInitialElementsMap);
                    
          
          if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
            outputAbstractReachabilityTree("art_" + lLoopCounter + "_c_", lRoot, lInitialElementsMap.keySet());
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
    
    printTestGoals("Infeasible test goals (#" + lInfeasibleTestGoals.size() + ") = ", lInfeasibleTestGoals);
   
    for (List<AbstractElementWithLocation> p : lPaths) {
      List<String> strpath = AbstractPathToCTranslator.translatePath(pCfas, AbstractPathToCTranslator.getPath(p, null));
      String lFunctionName = p.get(0).getLocationNode().getFunctionName(); 
      //assert(CProver.isFeasible(p.get(0).getLocationNode().getFunctionName(), strpath));
      FShell.isFeasible(strpath, lFunctionName + "_0");
    }
    
    System.out.println("#Test cases computed: " + lPaths.size());
    
    return null;
  }
  
  public static void propagate(QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    if (isPropagateable(pElement)) {
      // pElement will be no initial element anymore
      pInitialElementsMap.remove(pElement);
    }
    else {
      if (!pInitialElementsMap.containsKey(pElement)) {
        // set as initial element
      
        Set<CFAEdge> lEdges = getOutgoingCFAEdges(pElement);
        
        lEdges.removeAll(getVisitedCFAEdges(pElement));
                
        pInitialElementsMap.put(pElement, lEdges);
      }
    }
    
    for (Edge lEdge : pElement.getChildren()) {
      propagate(lEdge.getChild(), pInitialElementsMap);
    }
  }
  
  public static boolean isPropagateable(QDPTCompositeElement pElement) {
    assert(pElement != null);

    if (pElement.getNumberOfChildren() == 0) {
      return false;
    }
    
    CFANode lCFANode = pElement.getLocationNode();
    
    return (lCFANode.getNumLeavingEdges() == getVisitedCFAEdges(pElement).size());
  }

  public static Set<CFAEdge> getOutgoingCFAEdges(QDPTCompositeElement pElement) {
    assert(pElement != null);
    
    return getOutgoingCFAEdges(pElement.getLocationNode());
  }
  
  public static Set<CFAEdge> getOutgoingCFAEdges(CFANode pCFANode) {
    assert(pCFANode != null);
    
    HashSet<CFAEdge> lOutgoingEdges = new HashSet<CFAEdge>();

    for (int lIndex = 0; lIndex < pCFANode.getNumLeavingEdges(); lIndex++) {
      lOutgoingEdges.add(pCFANode.getLeavingEdge(lIndex));
    }

    return lOutgoingEdges;
  }
  
  public static Set<CFAEdge> getVisitedCFAEdges(QDPTCompositeElement pElement) {
    assert(pElement != null);
    
    HashSet<CFAEdge> lVisitedCFAEdges = new HashSet<CFAEdge>();
    
    for (Edge lEdge : pElement.getChildren()) {
      addVisitedCFAEdges(lEdge, lVisitedCFAEdges);
    }
    
    return lVisitedCFAEdges;
  }
  
  private static void addVisitedCFAEdges(Edge pEdge, Set<CFAEdge> pVisitedCFAEdges) {
    assert(pEdge != null);
    assert(pVisitedCFAEdges != null);
    
    if (pEdge instanceof QDPTCompositeCPA.CFAEdgeEdge) {
      pVisitedCFAEdges.add(((QDPTCompositeCPA.CFAEdgeEdge) pEdge).getCFAEdge());
    } 
    else {
      assert (pEdge instanceof QDPTCompositeCPA.SubpathsEdge);

      QDPTCompositeCPA.SubpathsEdge lSubpathsEdge = (QDPTCompositeCPA.SubpathsEdge) pEdge;

      for (List<Edge> lSubpath : lSubpathsEdge.getSubpaths()) {
        Edge lFirstEdge = lSubpath.get(0);
        
        addVisitedCFAEdges(lFirstEdge, pVisitedCFAEdges);
      }
    }
  }
  
  public static boolean mergePaths(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pCPA != null);
    assert(pTestGoalCPA != null);
    assert(pElement != null);
    
    
    boolean lARTHasBeenUpdated = false;
    
    for (Edge lEdge : pElement.getChildren()) {
      // merge paths in subtrees
      lARTHasBeenUpdated |= mergePaths(pCPA, pTestGoalCPA, lEdge.getChild(), pInitialElementsMap);
    }
    
    
    QDPTCompositeElement lMergeElement = null;
    
    if (pElement.getNumberOfChildren() > 1) {
      // pElement is a candidate for a merging point
      
      Vector<LinkedList<Edge>> lPaths = getSubpaths(pElement);
      
      if (lPaths.size() > 1) {
        Set<List<Edge>> lMergeSubpaths = getMergeSubpaths(pTestGoalCPA, lPaths);
        
        if (lMergeSubpaths.size() > 1) {
          lMergeElement = merge(pCPA, pTestGoalCPA, pElement, lMergeSubpaths, pInitialElementsMap);
          
          lARTHasBeenUpdated = true;
        }
      }
    }
    
    
    if (lMergeElement != null) {
      // we have merged and now this could be a new starting point for
      // merging paths
      
      if (lMergeElement.getNumberOfChildren() > 1) {
        // we don't need lARTHasBeenUpdated |= here, since merging was done
        // and thus lARTHasBeenUpdated is true anyway
        mergePaths(pCPA, pTestGoalCPA, lMergeElement, pInitialElementsMap);
      }
    }
    
    
    return lARTHasBeenUpdated;
  }
  
  public static QDPTCompositeElement merge(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement, Set<List<Edge>> pSubpaths, Map<QDPTCompositeElement, Set<CFAEdge>> pInitialElementsMap) {
    assert(pCPA != null);
    assert(pTestGoalCPA != null);
    assert(pElement != null);
    assert(pSubpaths != null);
    assert(pSubpaths.size() > 1);
    assert(pInitialElementsMap != null);
    
    List<Edge> lFirstSubpath = pSubpaths.iterator().next();
    
    assert(lFirstSubpath != null);
    
    Edge lLastEdge = lFirstSubpath.get(lFirstSubpath.size() - 1);
    
    QDPTCompositeElement lLastElement = lLastEdge.getChild();
    
    
    
    // create merge element
    List<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();
    // location cpa
    lAbstractElements.add(lLastElement.get(mLocationCPAIndex));
    // scope restriction cpa
    // TODO this has to be changed to handle scope restriction analysis correctly!
    lAbstractElements.add(lLastElement.get(mScopeRestrictionCPAIndex));
    // test goal cpa
    Set<Automaton<CFAEdge>.State> lNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();

    for (Automaton<CFAEdge>.State lState : pTestGoalCPA.getAbstractDomain().castToStateSetElement(lLastElement.get(mTestGoalCPAIndex)).getStates()) {
      if (!lState.isFinal()) {
        lNonAcceptingStates.add(lState);
      }
    }

    AutomatonCPADomain<CFAEdge>.StateSetElement lStateSetElement = pTestGoalCPA.getAbstractDomain().createStateSetElement(pTestGoalCPA.getAbstractDomain(), lNonAcceptingStates);
    lAbstractElements.add(lStateSetElement);

    pElement.hideChildren();

    QDPTCompositeElement lMergeElement = pCPA.createElement(lAbstractElements, pElement.getCallStack(), pElement, pSubpaths);

    Set<Edge> lEdgeSet = new HashSet<Edge>();

    
    for (List<Edge> lSubpath : pSubpaths) {
      Edge lEdge = lSubpath.get(lSubpath.size() - 1);
      
      QDPTCompositeElement lLastSubpathElement = lEdge.getChild();
      
      for (Edge lChildEdge : lLastSubpathElement.getChildren()) {
        lEdgeSet.add(lChildEdge);
      }
      
      // handle initial elements
      if (pInitialElementsMap.containsKey(lLastSubpathElement)) {
        Set<CFAEdge> lCFAEdges = pInitialElementsMap.get(lLastSubpathElement);
        
        pInitialElementsMap.remove(lLastSubpathElement);
        
        if (pInitialElementsMap.containsKey(lMergeElement)) {
          Set<CFAEdge> lCurrentCFAEdges = pInitialElementsMap.get(lMergeElement);
          
          lCurrentCFAEdges.addAll(lCFAEdges);
        }
        else {
          pInitialElementsMap.put(lMergeElement, lCFAEdges);
        }
      }
    }
          
    for (Edge lEdge : lEdgeSet) {
      lEdge.setParent(lMergeElement);
    }
    
    // merging could invalidate our minimality invariant for inital elements
    if (pInitialElementsMap.containsKey(lMergeElement)) {
      // we have to ensure that we will only visit previously unvisited edges
      Set<CFAEdge> lCFAEdges = pInitialElementsMap.get(lMergeElement);
      
      Set<CFAEdge> lVisitedCFAEdges = getVisitedCFAEdges(lMergeElement);
      
      lCFAEdges.removeAll(lVisitedCFAEdges);
      
      // NOTE: If there is no edge remaining we still need to have lMergeElements
      // in pInitialElementsMap for later propagation of initial elements status.
    }
    
    return lMergeElement;
  }
  
  public static Vector<LinkedList<Edge>> getSubpaths(QDPTCompositeElement pElement) {
    assert(pElement != null);
    
    Vector<LinkedList<Edge>> lPaths = new Vector<LinkedList<Edge>>();
    
    for (Edge lCurrentEdge : pElement.getChildren()) {
      LinkedList<Edge> lPath = new LinkedList<Edge>();
      
      do {
        QDPTCompositeElement lParent = lCurrentEdge.getParent();
        QDPTCompositeElement lChild = lCurrentEdge.getChild();
        
        // we do only intraprocedural merging
        // TODO allow interprocedural merging
        if (!lParent.getCallStack().equals(lChild.getCallStack())) {
          break;
        }
        
        lPath.add(lCurrentEdge);
        
        // since we do bottom up merging we can stop here, otherwise merging
        // would have happened
        if (lChild.getNumberOfChildren() != 1) {
          break;
        }
        
        // this is a possible new initial element
        // TODO put in some reasoning here to prevent from unnecessary cutting
        // of paths
        // TODO in cases all cfa edges are explored and lCurrentEdge is already
        // merged, we don't have to stop here
        if (lChild.getElementWithLocation().getLocationNode().getNumLeavingEdges() > 1) {
          break;
        }
        
        // we know there is only one successor edge
        lCurrentEdge = lChild.getChildren().iterator().next();
      } while (true);
      
      if (lPath.size() > 0) {
        lPaths.add(lPath);
      }
    }
    
    return lPaths;
  }
  
  public static boolean areElementsMergeable(TestGoalCPA pTestGoalCPA, QDPTCompositeElement pElement1, QDPTCompositeElement pElement2) {
    assert(pElement1 != null);
    assert(pElement2 != null);
    
    // TODO include equivalence test for scope restriction automaton
    
    if (!(pElement1.get(mLocationCPAIndex).equals(pElement2.get(mLocationCPAIndex)))) {
      return false;
    }
    
    if (!pElement1.getCallStack().equals(pElement2.getCallStack())) {
      return false;
    }

    
    AutomatonCPADomain<CFAEdge>.StateSetElement lCurrentStateSetElement = pTestGoalCPA.getAbstractDomain().castToStateSetElement(pElement1.get(mTestGoalCPAIndex));
    Set<Automaton<CFAEdge>.State> lCurrentStates = lCurrentStateSetElement.getStates();
    // TODO: provide this sets by automaton or test goal cpa element
    Set<Automaton<CFAEdge>.State> lCurrentNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();

    for (Automaton<CFAEdge>.State lState : lCurrentStates) {
      if (!lState.isFinal()) {
        lCurrentNonAcceptingStates.add(lState);
      }
    }

    
    AutomatonCPADomain<CFAEdge>.StateSetElement lCandidateStateSetElement = pTestGoalCPA.getAbstractDomain().castToStateSetElement(pElement2.get(mTestGoalCPAIndex));
    Set<Automaton<CFAEdge>.State> lCandidateStates = lCandidateStateSetElement.getStates();
    Set<Automaton<CFAEdge>.State> lCandidateNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();

    for (Automaton<CFAEdge>.State lState : lCandidateStates) {
      if (!lState.isFinal()) {
        lCandidateNonAcceptingStates.add(lState);
      }
    }
    
          
    return lCurrentNonAcceptingStates.equals(lCandidateNonAcceptingStates);
  }
  
  public static Set<List<Edge>> getMergeSubpaths(TestGoalCPA pTestGoalCPA, Vector<LinkedList<Edge>> pPaths) {
    assert(pPaths != null);
    
    
    // detect merging points
    LinkedList<QDPTCompositeElement[]>  lMatchingElementsTuples = new LinkedList<QDPTCompositeElement[]>();
    
    LinkedList<Edge> lFirstPath = pPaths.get(0);
    
    for (Edge lCurrentEdge : lFirstPath) {
      QDPTCompositeElement lCurrentElement = lCurrentEdge.getChild();
      
      QDPTCompositeElement[] lMatchingElementsTuple = new QDPTCompositeElement[pPaths.size()];
      
      lMatchingElementsTuple[0] = lCurrentElement;
      
      boolean lOk = true;
      
      for (int lPathIndex = 1; lPathIndex < pPaths.size(); lPathIndex++) {
        LinkedList<Edge> lCurrentPath = pPaths.get(lPathIndex);
        
        for (Edge lCandidateEdge : lCurrentPath) {
          QDPTCompositeElement lCandidateElement = lCandidateEdge.getChild();
          
          if (areElementsMergeable(pTestGoalCPA, lCurrentElement, lCandidateElement)) {
            lMatchingElementsTuple[lPathIndex] = lCandidateElement;
            
            break;
          }
        }
        
        if (lMatchingElementsTuple[lPathIndex] == null) {
          lOk = false;
          
          break;
        }
      }
      
      if (lOk) {
        lMatchingElementsTuples.add(lMatchingElementsTuple);
      }
    }
    
    
    if (lMatchingElementsTuples.size() <= 0) {
      // nothing to merge so return
      return Collections.EMPTY_SET;
    }
    
    
    int lMinDepth = Integer.MAX_VALUE;

    QDPTCompositeElement[] lFinalTuple = null;

    for (QDPTCompositeElement[] lTuple : lMatchingElementsTuples) {
      int lCurrentMinDepth = Integer.MAX_VALUE;

      for (QDPTCompositeElement lElement : lTuple) {
        int lTmpDepth = lElement.getDepth();

        if (lElement.getDepth() < lCurrentMinDepth) {
          lCurrentMinDepth = lTmpDepth;
        }
      }

      if (lMinDepth > lCurrentMinDepth) {
        lFinalTuple = lTuple;
        lMinDepth = lCurrentMinDepth;
      }
    }

    assert (lFinalTuple != null);
            
    
    // determine paths
    HashSet<List<Edge>> lMergePaths = new HashSet<List<Edge>>();    

    for (int lMergePathIndex = 0; lMergePathIndex < pPaths.size(); lMergePathIndex++) {
      LinkedList<Edge> lTmpPath = new LinkedList<Edge>();

      for (Edge lMergeEdge : pPaths.get(lMergePathIndex)) {
        if (lMergeEdge.getChild().equals(lFinalTuple[lMergePathIndex])) {
          lTmpPath.add(lMergeEdge);

          break;
        }

        lTmpPath.addLast(lMergeEdge);
      }

      lMergePaths.add(lTmpPath);
    }
    
    return lMergePaths;
  }
  
  /*
   * Returns true if the abstract reachability tree was changed, otherwise it
   * returns false. 
   */
  public static boolean rearrangeAbstractReachabilityTree(QDPTCompositeCPA pCPA, TestGoalCPA pTestGoalCPA, QDPTCompositeElement pRoot, Set<QDPTCompositeElement> pInitialElements) {
    assert(pRoot != null);
    assert(pInitialElements != null);
    
    if (!pRoot.hasChildren()) {
      // we have no successor, so ART stays unchanged
      return false;
    }
    
    
    if (pRoot.getNumberOfChildren() == 1) {
      // we have exactly one successor
      Iterator<Edge> lChildren = pRoot.getChildren().iterator();
      
      return rearrangeAbstractReachabilityTree(pCPA, pTestGoalCPA, lChildren.next().getChild(), pInitialElements);
    }
    
    // we have more than one successor

    Vector<LinkedList<Edge>> lPaths = new Vector<LinkedList<Edge>>();
    
    Iterator<Edge> lChildren = pRoot.getChildren().iterator();
    
    while (lChildren.hasNext()) {
      LinkedList<Edge> lPath = new LinkedList<Edge>();
      
      Edge lCurrentEdge = lChildren.next();
      
      lPath.add(lCurrentEdge);
      
      QDPTCompositeElement lChild = lCurrentEdge.getChild();
      
      while (lChild.getNumberOfChildren() == 1) {
        lCurrentEdge = lChild.getChildren().iterator().next();
        
        lPath.addLast(lCurrentEdge);
        
        lChild = lCurrentEdge.getChild();
      }
      
      lPaths.add(lPath);
    }
    
    if (lPaths.size() <= 1) {
      return false;
    }
    
    
    boolean lWasUpdated = false;
    
    
    // detect merging points
    LinkedList<QDPTCompositeElement[]>  lMatchingElementsTuples = new LinkedList<QDPTCompositeElement[]>();
    
    LinkedList<Edge> lFirstPath = lPaths.get(0);
    
    for (Edge lCurrentEdge : lFirstPath) {
      QDPTCompositeElement lCurrentElement = lCurrentEdge.getChild();
      
      LocationElement lCurrentLocationElement = (LocationElement)lCurrentElement.get(mLocationCPAIndex);
      AutomatonCPADomain<CFAEdge>.StateSetElement lCurrentStateSetElement = pTestGoalCPA.getAbstractDomain().castToStateSetElement(lCurrentElement.get(mTestGoalCPAIndex));
      Set<Automaton<CFAEdge>.State> lCurrentStates = lCurrentStateSetElement.getStates();
      
      // TODO: provide this sets by automaton or test goal cpa element
      Set<Automaton<CFAEdge>.State> lCurrentNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();
      
      for (Automaton<CFAEdge>.State lState : lCurrentStates) {
        if (!lState.isFinal()) {
          lCurrentNonAcceptingStates.add(lState);
        }
      }
      
      QDPTCompositeElement[] lMatchingElementsTuple = new QDPTCompositeElement[lPaths.size()];
      
      lMatchingElementsTuple[0] = lCurrentElement;
      
      for (int lPathIndex = 1; lPathIndex < lPaths.size(); lPathIndex++) {
        LinkedList<Edge> lCurrentPath = lPaths.get(lPathIndex);
        
        for (Edge lCandidateEdge : lCurrentPath) {
          QDPTCompositeElement lCandidateElement = lCandidateEdge.getChild();
          
          // check for equivalence
          if (!lCurrentLocationElement.equals(lCandidateElement.get(mLocationCPAIndex))) {
            continue;
          }
          
          if (!lCurrentElement.getCallStack().equals(lCandidateElement.getCallStack())) {
            continue;
          }
          
          AutomatonCPADomain<CFAEdge>.StateSetElement lCandidateStateSetElement = pTestGoalCPA.getAbstractDomain().castToStateSetElement(lCandidateElement.get(mTestGoalCPAIndex));
          
          Set<Automaton<CFAEdge>.State> lCandidateStates = lCandidateStateSetElement.getStates();
          
          Set<Automaton<CFAEdge>.State> lCandidateNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();
          
          for (Automaton<CFAEdge>.State lState : lCandidateStates) {
            if (!lState.isFinal()) {
              lCandidateNonAcceptingStates.add(lState);
            }
          }
          
          if (lCurrentNonAcceptingStates.equals(lCandidateNonAcceptingStates)) {
            lMatchingElementsTuple[lPathIndex] = lCandidateElement;
            
            break;
          }
        }
      }
      
      boolean lOk = true;
      
      for (QDPTCompositeElement lElement : lMatchingElementsTuple) {
        if (lElement == null) {
          lOk = false;
        }
      }
      
      if (lOk) {
        lMatchingElementsTuples.add(lMatchingElementsTuple);
      }
    }
    
    if (lMatchingElementsTuples.size() > 0) {
      int lMinDepth = Integer.MAX_VALUE;
      
      QDPTCompositeElement[] lFinalTuple = null;
      
      for (QDPTCompositeElement[] lTuple : lMatchingElementsTuples) {
        int lCurrentMinDepth = Integer.MAX_VALUE;
        
        for (QDPTCompositeElement lElement : lTuple) {
          int lTmpDepth = lElement.getDepth();
          
          if (lElement.getDepth() < lCurrentMinDepth) {
            lCurrentMinDepth = lTmpDepth;
          }
        }
        
        if (lMinDepth > lCurrentMinDepth) {
          lFinalTuple = lTuple;
          lMinDepth = lCurrentMinDepth;
        }
      }
      
      assert(lFinalTuple != null);
            
      
      
      // determine paths
      HashSet<List<Edge>> lMergePaths = new HashSet<List<Edge>>();
      
      for (int lMergePathIndex = 0; lMergePathIndex < lPaths.size(); lMergePathIndex++) {
        LinkedList<Edge> lTmpPath = new LinkedList<Edge>();
        
        for (Edge lMergeEdge : lPaths.get(lMergePathIndex)) {
          if (lMergeEdge.getChild().equals(lFinalTuple[lMergePathIndex])) {
            lTmpPath.add(lMergeEdge);
            
            break;
          }
          
          lTmpPath.addLast(lMergeEdge);
        }
        
        lMergePaths.add(lTmpPath);
      }
      
      
      // create merge element
      List<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();
      // location cpa
      lAbstractElements.add(lFinalTuple[0].get(mLocationCPAIndex));
      // scope restriction cpa
      // TODO this has to be changed to handle scope restriction analysis correctly!
      lAbstractElements.add(lFinalTuple[0].get(mScopeRestrictionCPAIndex));
      // test goal cpa
      Set<Automaton<CFAEdge>.State> lNonAcceptingStates = new HashSet<Automaton<CFAEdge>.State>();
          
      for (Automaton<CFAEdge>.State lState : pTestGoalCPA.getAbstractDomain().castToStateSetElement(lFinalTuple[0].get(mTestGoalCPAIndex)).getStates()) {
        if (!lState.isFinal()) {
          lNonAcceptingStates.add(lState);
        }
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lStateSetElement = pTestGoalCPA.getAbstractDomain().createStateSetElement(pTestGoalCPA.getAbstractDomain(), lNonAcceptingStates);
      lAbstractElements.add(lStateSetElement);
      
      pRoot.hideChildren();
      
      QDPTCompositeElement lMergeElement = pCPA.createElement(lAbstractElements, lFinalTuple[0].getCallStack(), pRoot, lMergePaths);
      
      Set<Edge> lEdgeSet = new HashSet<Edge>();
      
      for (QDPTCompositeElement lElement : lFinalTuple) {
        for (Edge lEdge : lElement.getChildren()) {
          lEdgeSet.add(lEdge);
        }
        
        if (pInitialElements.contains(lElement)) {
          pInitialElements.remove(lElement);
          pInitialElements.add(lMergeElement);
        }
      }
      
      lWasUpdated = true;
      
      for (Edge lEdge : lEdgeSet) {
        lEdge.setParent(lMergeElement);
      }

      propagateInitialElements(lMergeElement, pInitialElements);

      rearrangeAbstractReachabilityTree(pCPA, pTestGoalCPA, lMergeElement, pInitialElements);
    }
    else {
      for (LinkedList<Edge> lPath : lPaths) {
        lWasUpdated |= rearrangeAbstractReachabilityTree(pCPA, pTestGoalCPA, lPath.getLast().getChild(), pInitialElements);
      }
    }
    
    return lWasUpdated;
  }
  
  public static void propagateInitialElements(QDPTCompositeElement pRoot, Set<QDPTCompositeElement> pInitialElements) {
    assert(pRoot != null);
    assert(pInitialElements != null);
    
    if (pInitialElements.contains(pRoot)) {
      // check whether all successors are enumerated
      Set<CFAEdge> lLeavingEdges = new HashSet<CFAEdge>();

      for (Edge lEdge : pRoot.getChildren()) {
        assert(lEdge instanceof QDPTCompositeCPA.CFAEdgeEdge);
        
        CFAEdge lCFAEdge = ((QDPTCompositeCPA.CFAEdgeEdge)lEdge).getCFAEdge();

        lLeavingEdges.add(lCFAEdge);
      }
      
      CFANode lCFANode = pRoot.getElementWithLocation().getLocationNode();

      // is this check enough for ensuring correct exploration of successors?
      if (lLeavingEdges.size() == lCFANode.getNumLeavingEdges()) {
        pInitialElements.remove(pRoot);
        
        for (Edge lEdge : pRoot.getChildren()) {
          pInitialElements.add(lEdge.getChild());
          
          propagateInitialElements(lEdge.getChild(), pInitialElements);
        }
      }
      else {
        // remove all children from ART
        Set<QDPTCompositeElement> lChildren = new HashSet<QDPTCompositeElement>();
        
        for (Edge lEdge : pRoot.getChildren()) {
          lChildren.add(lEdge.getChild());
        }
        
        for (QDPTCompositeElement lChild : lChildren) {
          removeFromInitialElements(lChild, pInitialElements);
        }
      }
    }
  }
  
  public static void removeFromInitialElements(QDPTCompositeElement pRoot, Set<QDPTCompositeElement> pInitialElements) {
    assert(pRoot != null);
    assert(pInitialElements != null);
    
    pInitialElements.remove(pRoot);
    
    for (Edge lEdge : pRoot.getChildren()) {
      removeFromInitialElements(lEdge.getChild(), pInitialElements);
    }
  }
  
  public static void outputAbstractReachabilityTree(String pFileId, QDPTCompositeElement pRoot, Collection<QDPTCompositeElement> pSpecialElements) {
    assert(pFileId != null);
    assert(pRoot != null);
    assert(pSpecialElements != null);
    
    File lFile = null;

    try {
      lFile = File.createTempFile(pFileId, ".dot");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    //lFile.deleteOnExit();

    List<String> lNodeDefinitions = new LinkedList<String>();
    
    List<String> lEdgeDefinitions = new LinkedList<String>();
    
    Stack<QDPTCompositeElement> lWorklist = new Stack<QDPTCompositeElement>();
    lWorklist.push(pRoot);
    
    int lUniqueId = 0;
    
    Map<QDPTCompositeElement, Integer> lIdMap = new HashMap<QDPTCompositeElement, Integer>();
    
    // putting ids into map
    while (!lWorklist.empty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.pop();
      
      lIdMap.put(lCurrentElement, lUniqueId);
      
      for (Edge lEdge : lCurrentElement.getChildren()) {
        lWorklist.push(lEdge.getChild());
      }
      
      if (pSpecialElements.contains(lCurrentElement)) {
        lNodeDefinitions.add("node [label = \"<" + lCurrentElement + ", " + lCurrentElement.getDepth() + ">\", shape=diamond, fillcolor=yellow, style=filled]; " + (lUniqueId++) + ";");
      }
      else {
        lNodeDefinitions.add("node [label = \"<" + lCurrentElement + ", " + lCurrentElement.getDepth() + ">\", shape=box, fillcolor=white]; " + (lUniqueId++) + ";");
      }
    }
    
    
    lWorklist.push(pRoot);
    while (!lWorklist.empty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.pop();
      
      Integer lId = lIdMap.get(lCurrentElement);
      
      lNodeDefinitions.add("node [label = \"" + lCurrentElement + "\", shape=box]; " + lId + ";");

      for (Edge lEdge : lCurrentElement.getChildren()) {
        QDPTCompositeElement lChildElement = lEdge.getChild();
        
        lWorklist.push(lChildElement);
        
        if (lEdge instanceof QDPTCompositeCPA.HasSubpaths) {
          if (lEdge instanceof QDPTCompositeCPA.CFAEdgeAndSubpathsEdge) {
            lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + " [style=bold, color=green];");
          }
          else {
            lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + " [style=bold, color=blue];");
          }
        }
        else {
          lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + ";");
        }
      }
    }
    
    try {
      PrintWriter lWriter = new PrintWriter(lFile);
      
      lWriter.println("digraph ART {");
      
      //lWriter.println("size=\"6,10\";");
      
      for (String lString : lNodeDefinitions) {
        lWriter.println(lString);
      }
      
      for (String lString : lEdgeDefinitions) {
        lWriter.println(lString);
      }
      
      lWriter.print("}");

      lWriter.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }
    
    

    /*try {
      File lPostscriptFile = File.createTempFile(pFileId, ".ps");

      lPostscriptFile.deleteOnExit();

      Process lDotProcess = Runtime.getRuntime().exec("dot -Tps -o" + lPostscriptFile.getAbsolutePath() + " " + lFile.getAbsolutePath());

      lDotProcess.waitFor();

      File lPDFFile = File.createTempFile(pFileId, ".pdf");

      Process lPs2PdfProcess = Runtime.getRuntime().exec("ps2pdf " + lPostscriptFile.getAbsolutePath() + " " + lPDFFile.getAbsolutePath());

      lPs2PdfProcess.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
      assert (false);
    }*/
  }
  
  public static void outputAbstractReachabilityTree(String pFileId, Collection<CompositeElement> pSpecialElements, ParametricAbstractReachabilityTree<CompositeElement> pAbstractReachabilityTree) {
    assert(pAbstractReachabilityTree != null);
    assert(pFileId != null);
    assert(pSpecialElements != null);
    
    File lFile = null;

    try {
      lFile = File.createTempFile(pFileId, ".dot");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    lFile.deleteOnExit();

    PrintWriter lWriter = null;

    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }

    lWriter.println(pAbstractReachabilityTree.toDot(pSpecialElements));

    lWriter.close();

    try {
      File lPostscriptFile = File.createTempFile(pFileId, ".ps");

      lPostscriptFile.deleteOnExit();

      Process lDotProcess = Runtime.getRuntime().exec("dot -Tps -o" + lPostscriptFile.getAbsolutePath() + " " + lFile.getAbsolutePath());

      lDotProcess.waitFor();

      File lPDFFile = File.createTempFile(pFileId, ".pdf");

      Process lPs2PdfProcess = Runtime.getRuntime().exec("ps2pdf " + lPostscriptFile.getAbsolutePath() + " " + lPDFFile.getAbsolutePath());

      lPs2PdfProcess.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
      assert (false);
    }
  }
  
  public static void printTestGoals(String pTitle, Collection<Automaton<CFAEdge>.State> pTestGoals) {
    System.out.print(pTitle);

    printTestGoals(pTestGoals);
  }
  
  public static void printTestGoals(Collection<Automaton<CFAEdge>.State> pTestGoals) {
    boolean lFirstTestGoal = true;

    System.out.print("{");

    for (Automaton<CFAEdge>.State lTestGoal : pTestGoals) {
      if (lFirstTestGoal) {
        lFirstTestGoal = false;
      } else {
        System.out.print(",");
      }

      System.out.print("q" + lTestGoal.getIndex());
    }

    System.out.println("}");
  }
}
