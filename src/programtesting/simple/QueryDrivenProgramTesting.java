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
import common.Pair;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
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
          
    Set<QDPTCompositeElement> lInitialElements = new HashSet<QDPTCompositeElement>();
    
    lInitialElements.add(cpa.getInitialElement(pMainFunction));

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
      
      
      
      Set<QDPTCompositeElement> lOldInitialElements = new HashSet<QDPTCompositeElement>(lInitialElements);

      try {
        algo.CPAWithInitialSet(cpa, lInitialElements, lInitialPrecision);
        
        lInitialElements.clear();
      } catch (CPAException e1) {
        e1.printStackTrace();
        assert(false);
      }
       
      assert(cpa.getTransferRelation().hasRoot());
      
      QDPTCompositeElement lRoot = cpa.getTransferRelation().getRoot();
  
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        outputAbstractReachabilityTree("art_" + lLoopCounter + "_a_", lRoot, lOldInitialElements);
      }
      
      LinkedList<Edge> lWorklist = new LinkedList<Edge>();
      
      for (CompositeElement lInitialElement : lOldInitialElements) {
        QDPTCompositeElement lElement = (QDPTCompositeElement)lInitialElement;
        
        Iterator<Edge> lChildrenIterator = lElement.getChildren();
        
        while (lChildrenIterator.hasNext()) {
          Edge lEdge = lChildrenIterator.next();
          
          lWorklist.add(lEdge);
        }
      }
      
      int lPathCounter = 0;
      int lPathMaxLength = 0;
      
      while (!lWorklist.isEmpty() && !lTestGoals.isEmpty()) {
        Edge lCurrentEdge = lWorklist.removeFirst();
        
        QDPTCompositeElement lCurrentElement = lCurrentEdge.getChild();
        
        if (lCurrentElement.hasChildren()) {
          // we are at an intermediate node
          
          Iterator<Edge> lChildrenIterator = lCurrentElement.getChildren();
          
          while (lChildrenIterator.hasNext()) {
            Edge lEdge = lChildrenIterator.next();
            
            // we do a depth first traversal
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
          
          // temporary hack
          // TODO reimplement feasiblity check
          /*LinkedList<CFAEdge> lPath = new LinkedList<CFAEdge>();
          
          for (Edge lEdge : lPathToRoot) {
            assert(!lEdge.hasSubpaths());
            
            lPath.add(lEdge.getCFAEdge());
          }*/
          
          boolean lFeasible = false;

          Edge lInfeasibilityCause = null;

          int lCallsToCBMCCounter = 0;
          
          QDPTCompositeElement lLastFeasibleElement = lCurrentElement;
          
          HashSet<Edge> lBacktrackingSet = new HashSet<Edge>();
          
          do {
            //System.out.println(lPathToRoot);
              
            //System.out.println(lTranslator.translate(lPathToRoot));
              
            //assert(false);
            
            // TODO getPath has to be only called once, afterwards we can
            // directly manipulate lCFAPath
            //List<String> lPathStringRepresentation = AbstractPathToCTranslator.translatePath(pCfas, lPath);

            String lPathCSource = lTranslator.translate(lPathToRoot);
            
            lCallsToCBMCCounter++;
            //lFeasible = CProver.isFeasible(lPath.get(0).getPredecessor().getFunctionName(), lPathStringRepresentation);
            lFeasible = CProver.isFeasible(lRoot.getLocationNode().getFunctionName(), lPathCSource);

            if (!lFeasible) {
              // what's about function pointers?
              int lRemoveIndex = lPathToRoot.size() - 1;
              Edge lRemoveEdge = lPathToRoot.get(lPathToRoot.size() - 1);
              
              if (lInfeasibilityCause != null) {
                Iterator<Edge> lBacktrackChildren = lInfeasibilityCause.getParent().getChildren();

                while (lBacktrackChildren.hasNext()) {
                  lBacktrackingSet.add(lBacktrackChildren.next());
                }
              }
              
              while (lRemoveEdge.getCFAEdge().getEdgeType() != CFAEdgeType.AssumeEdge) {
                lPathToRoot.remove(lRemoveIndex);
                //lPath.remove(lRemoveIndex);
                
                // If parent has not more than one child then this edge is no
                // longer in the worklist because it was processed already.
                if (lRemoveEdge.getParent().getNumberOfChildren() > 1) {
                  Iterator<Edge> lBacktrackChildren = lRemoveEdge.getParent().getChildren();

                  while (lBacktrackChildren.hasNext()) {
                    lBacktrackingSet.add(lBacktrackChildren.next());
                  }
                }
                
                lRemoveIndex--;
                lRemoveEdge = lPathToRoot.get(lRemoveIndex);
              }
              
              //List<String> lTmpPathStringRepresentation = AbstractPathToCTranslator.translatePath(pCfas, lPath);

              lPathCSource = lTranslator.translate(lPathToRoot);
              
              // TODO remove this from production code -> lTmpFeasible stuff
              //lCallsToCBMCCounter++;
              //boolean lTmpFeasible = CProver.isFeasible(lPath.get(0).getPredecessor().getFunctionName(), lTmpPathStringRepresentation);
              boolean lTmpFeasible = CProver.isFeasible(lRoot.getLocationNode().getFunctionName(), lPathCSource);

              assert (!lTmpFeasible);

              // remove assume edge
              lInfeasibilityCause = lPathToRoot.remove(lPathToRoot.size() - 1);
              //lPath.remove(lPath.size() - 1);
              
              lLastFeasibleElement = lInfeasibilityCause.getParent();
            }
          } while (!lFeasible);
          
          
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
            lInitialElements.add(lCurrentElement);
          }
          else {
            lInfeasibilityCause.getChild().remove();
          }
        }
      }
      
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        outputAbstractReachabilityTree("art_" + lLoopCounter + "_b_", lRoot, lInitialElements);
      }
      
      if (lInitialElements.isEmpty()) {
        // we have nothing to explore anymore, so all remaining test goals
        // are infeasible
        lInfeasibleTestGoals.addAll(lTestGoals);
        lTestGoals.clear();
      }
      else {
        if (!lTestGoals.isEmpty()) {
          rearrangeAbstractReachabilityTree(cpa, lTestGoalCPA, lRoot, lInitialElements);
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
      Iterator<Edge> lChildren = pRoot.getChildren();
      
      return rearrangeAbstractReachabilityTree(pCPA, pTestGoalCPA, lChildren.next().getChild(), pInitialElements);
    }
    
    
    // we have more than one successor
    
    System.out.println("ROOT: " + pRoot);
    
    
    
    Vector<LinkedList<Edge>> lPaths = new Vector<LinkedList<Edge>>();
    
    Iterator<Edge> lChildren = pRoot.getChildren();
    
    while (lChildren.hasNext()) {
      LinkedList<Edge> lPath = new LinkedList<Edge>();
      
      Edge lCurrentEdge = lChildren.next();
      
      lPath.add(lCurrentEdge);
      
      QDPTCompositeElement lChild = lCurrentEdge.getChild();
      
      while (lChild.getNumberOfChildren() == 1) {
        lCurrentEdge = lChild.getChildren().next();
        
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
      
      System.out.println(lMergePaths);
      
      System.out.println(lMergePaths.size());
      
      pRoot.hideChildren();
      
      QDPTCompositeElement lMergeElement = pCPA.createElement(lAbstractElements, lFinalTuple[0].getCallStack(), pRoot, lMergePaths);
      
      Set<Edge> lEdgeSet = new HashSet<Edge>();
      
      for (QDPTCompositeElement lElement : lFinalTuple) {
        Iterator<Edge> lIterator = lElement.getChildren();
        
        while (lIterator.hasNext()) {
          Edge lEdge = lIterator.next();
          
          lEdgeSet.add(lEdge);
        }
        
        if (pInitialElements.contains(lElement)) {
          pInitialElements.remove(lElement);
          pInitialElements.add(lMergeElement);
        }
      }
      
      for (Edge lEdge : lEdgeSet) {
        lEdge.changeParent(lMergeElement);
      }
      
      lWasUpdated |= rearrangeAbstractReachabilityTree(pCPA, pTestGoalCPA, lMergeElement, pInitialElements);
    }
    else {
      for (LinkedList<Edge> lPath : lPaths) {
        lWasUpdated |= rearrangeAbstractReachabilityTree(pCPA, pTestGoalCPA, lPath.getLast().getChild(), pInitialElements);
      }
    }
    
    return lWasUpdated;
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

    lFile.deleteOnExit();

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
      
      Iterator<Edge> lIterator = lCurrentElement.getChildren();
      
      while (lIterator.hasNext()) {
        lWorklist.push(lIterator.next().getChild());
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
      
      Iterator<Edge> lIterator = lCurrentElement.getChildren();
      
      while (lIterator.hasNext()) {
        QDPTCompositeElement lChildElement = lIterator.next().getChild();
        
        lWorklist.push(lChildElement);
        
        lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement));
      }
    }
    
    try {
      PrintWriter lWriter = new PrintWriter(lFile);
      
      lWriter.println("digraph ART {");
      
      lWriter.println("size=\"6,10\";");
      
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
