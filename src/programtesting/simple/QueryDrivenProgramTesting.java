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
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;
import exceptions.CPAException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {
  
  //private final static int mScopeRestrictionCPAIndex = 2;
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
          
    Set<CompositeElement> lInitialElements = new HashSet<CompositeElement>();
    
    lInitialElements.add(cpa.getInitialElement(pMainFunction));
    

    ParametricAbstractReachabilityTree<CompositeElement> lAbstractReachabilityTree = cpa.getTransferRelation().getAbstractReachabilityTree();
    // the resulting set of paths
    Set<List<AbstractElementWithLocation>> lPaths = new HashSet<List<AbstractElementWithLocation>>();
    
    FeasiblePathTree<AbstractElementWithLocation> lPathTree = new FeasiblePathTree<AbstractElementWithLocation>();
    
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
      
      
      
      Set<CompositeElement> lOldInitialElements = new HashSet<CompositeElement>(lInitialElements);

      lAbstractReachabilityTree.unsetUpdatedFlag();
      
      try {
        algo.CPAWithInitialSet(cpa, lInitialElements, lInitialPrecision);
        
        lInitialElements.clear();
      } catch (CPAException e1) {
        e1.printStackTrace();
        assert(false);
      }
      
      if (lAbstractReachabilityTree.hasBeenUpdated()) {
      
      
      
      // TODO Think about infeasible test goal handling
      // PROBLEM: Recursive calls and stop-Operator of call-stack analysis
      // TODO Remove this output
      //printTestGoals("Infeasible Test Goals: ", lTestGoalPrecision.getRemainingFinalStates());
      
      //lInfeasibleTestGoals.addAll(lTestGoalPrecision.getRemainingFinalStates());
      
      // Remove the infeasible test goals. If the set of remaining final states is
      // not empty this means that we have fully traversed an overapproximation
      // of the reachable state space. This shows that the remaing goals are not
      // reachable at all.
      //lTestGoals.removeAll(lTestGoalPrecision.getRemainingFinalStates());
      
      
      
      // process abstract reachability tree
      //ParametricAbstractReachabilityTree<CompositeElement> lAbstractReachabilityTree = cpa.getTransferRelation().getAbstractReachabilityTree();
      
      
      System.out.println("Size of ART: " + lAbstractReachabilityTree.size());
      
      
      
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
        outputAbstractReachabilityTree("art_" + lLoopCounter + "_", lOldInitialElements, lAbstractReachabilityTree);
      }
      
      
      assert(lAbstractReachabilityTree.hasRoot());
      
      CompositeElement lRoot = lAbstractReachabilityTree.getRoot();
      
      System.out.println("ROOT " + lRoot);
      
      Stack<Pair<CompositeElement, Iterator<CompositeElement>>> lStack = new Stack<Pair<CompositeElement, Iterator<CompositeElement>>>();
      
      assert(!lAbstractReachabilityTree.getChildren(lRoot).isEmpty());
      
      lStack.push(new Pair(lRoot, lAbstractReachabilityTree.getChildren(lRoot).iterator()));
      
      
      
      int lPathCounter = 0;
      int lPathMaxLength = 0;
      
      HashSet<CompositeElement> lElementsToBeRemovedFromART = new HashSet<CompositeElement>();

      while (!lStack.empty() && !lTestGoals.isEmpty()) {
        Pair<CompositeElement, Iterator<CompositeElement>> lCurrentPair = lStack.peek();

        Iterator<CompositeElement> lIterator = lCurrentPair.getSecond();
        
        if (lIterator.hasNext()) {
          CompositeElement lChild = lIterator.next();

          if (!lAbstractReachabilityTree.contains(lChild)) {
            // infeasible child
            // TODO remove from parents children
            continue;
          }
          
          Collection<CompositeElement> lGrandchildren = lAbstractReachabilityTree.getChildren(lChild);
          
          if (lGrandchildren.isEmpty()) {
            // we are at a leaf
            

            lPathCounter++;
            
            
            if (lStack.size() > lPathMaxLength) {
              lPathMaxLength = lStack.size();
            }

            AbstractElement lTmpElement = lChild.get(mTestGoalCPAIndex);

            // the ART should not contain the bottom element
            assert (!lTestGoalCPA.getAbstractDomain().isBottomElement(lTmpElement));

            // top element should never occur
            assert (!lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTmpElement));

            List<AbstractElementWithLocation> lPath = new ArrayList<AbstractElementWithLocation>(lStack.size() + 1);

            for (Pair<CompositeElement, Iterator<CompositeElement>> lStackElement : lStack) {
              lPath.add(lStackElement.getFirst());
            }

            lPath.add(lChild);


            boolean lFeasible = false;

            CompositeElement lInfeasibleElement = null;

            int lCallsToCBMCCounter = 0;
            
            if (true) {
              do {
                // TODO getPath has to be only called once, afterwards we can
                // directly manipulate lCFAPath
                List<CFAEdge> lCFAPath = AbstractPathToCTranslator.getPath(lPath);

                List<String> lPathStringRepresentation = AbstractPathToCTranslator.translatePath(pCfas, lCFAPath);

                lCallsToCBMCCounter++;
                lFeasible = CProver.isFeasible(lPath.get(0).getLocationNode().getFunctionName(), lPathStringRepresentation);
              
                if (!lFeasible) {
                  // what's about function pointers?
                  while (lCFAPath.get(lCFAPath.size() - 1).getEdgeType() != CFAEdgeType.AssumeEdge) {
                    lPath.remove(lPath.size() - 1);
                    lCFAPath.remove(lCFAPath.size() - 1);
                  }

                  List<String> lTmpPathStringRepresentation = AbstractPathToCTranslator.translatePath(pCfas, lCFAPath);

                  // TODO remove this from production code -> lTmpFeasible stuff
                  //lCallsToCBMCCounter++;
                  boolean lTmpFeasible = CProver.isFeasible(lPath.get(0).getLocationNode().getFunctionName(), lTmpPathStringRepresentation);

                  assert(!lTmpFeasible);

                  lInfeasibleElement = (CompositeElement)lPath.remove(lPath.size() - 1);
                }
              } while (!lFeasible);
            }
            else {
              int lKnownFeasible = -1;
              int lCurrentBoundary = lPath.size() - 1;
              int lUpperBound = lPath.size() - 1;
              
              
              while (true) {
                System.err.println("K C U: " + lKnownFeasible + " " + lCurrentBoundary + " " + lUpperBound);
                assert(lKnownFeasible < lCurrentBoundary);

                List<CFAEdge> lCFAPath = AbstractPathToCTranslator.getPath(lPath, lPath.get(lCurrentBoundary));

                List<String> lPathStringRepresentation = AbstractPathToCTranslator.translatePath(pCfas, lCFAPath);

                lCallsToCBMCCounter++;
                lFeasible = CProver.isFeasible(lPath.get(0).getLocationNode().getFunctionName(), lPathStringRepresentation);

                /*if (!lFeasible) {
                  --lCurrentBoundary;
                } else {
                  lKnownFeasible = lCurrentBoundary;
                  break;
                }*/
                
                // binary search
                if (!lFeasible) {
                  if ((lKnownFeasible + 1) == lCurrentBoundary) { // we have found the last usable element
                    break;
                  } else {
                    lUpperBound = lCurrentBoundary;
                    lCurrentBoundary = (lKnownFeasible + lCurrentBoundary) / 2;
                  }
                } else { 
                  if (lCurrentBoundary == (lPath.size() - 1)) { // path is ok in full
                    lKnownFeasible = lCurrentBoundary;
                    break;
                  } else if ((lCurrentBoundary + 1) == lUpperBound) { // we have found the last usable element
                    ++lKnownFeasible;
                    break;
                  } else {
                    lKnownFeasible = lCurrentBoundary;
                    lCurrentBoundary = (lUpperBound + lCurrentBoundary) / 2;
                  }
                }
              }
              
              System.err.println("final K C U: " + lKnownFeasible + " " + lCurrentBoundary + " " + lUpperBound);

              assert (lKnownFeasible >= 0);
              assert (lKnownFeasible < lPath.size());
              if (lKnownFeasible < (lPath.size() - 1)) {
                lInfeasibleElement = (CompositeElement) lPath.get(lKnownFeasible + 1);
                lPath = lPath.subList(0, lKnownFeasible + 1); // removeRange is a protected member only, would be nicer...
              }

              lPaths.add(lPath);
              lPathTree.addPath(lPath);
            }
            
            System.out.println(lPathCounter + "[" + lCallsToCBMCCounter + "]");
            

            CompositeElement lLastFeasibleElement = (CompositeElement) lPath.get(lPath.size() - 1);

            // + 1 comes from lChild that is NOT on the stack!
            if (lPath.size() == lStack.size() + 1) {
              lInitialElements.add(lLastFeasibleElement);
            }

            AbstractElement lBacktrackTmpElement = lLastFeasibleElement.get(mTestGoalCPAIndex);

            // the ART should not contain the bottom element
            // TODO move this out of feasibilty check to stack insertion
            assert (!lTestGoalCPA.getAbstractDomain().isBottomElement(lBacktrackTmpElement));

            // top element should never occur
            // TODO move this out of feasibilty check to stack insertion
            assert (!lTestGoalCPA.getAbstractDomain().getTopElement().equals(lBacktrackTmpElement));

            // now, we know it is an StateSetElement
            AutomatonCPADomain<CFAEdge>.StateSetElement lBacktrackTestGoalCPAElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lBacktrackTmpElement);

            final Set<Automaton<CFAEdge>.State> lBacktrackStates = lBacktrackTestGoalCPAElement.getStates();

            // remove the test goal from lTestGoals
            lTestGoals.removeAll(lBacktrackStates);


            // backtrack
            while (lStack.size() > lPath.size()) {
              lStack.pop();
            }

            // cleanup ART
            if (lInfeasibleElement != null) {
              lElementsToBeRemovedFromART.add(lInfeasibleElement);
            }
          } else {
            lStack.push(new Pair<CompositeElement, Iterator<CompositeElement>>(lChild, lAbstractReachabilityTree.getChildren(lChild).iterator()));
          }
        } else {
          // we are done with all children
          lStack.pop();
        }
      }
           
      for (CompositeElement lElement : lElementsToBeRemovedFromART) {
        lAbstractReachabilityTree.removeSubtree(lElement);
      }
      
      System.out.println();
      System.out.println("lStack.empty() = " + lStack.empty());
      System.out.println("lTestGoals.isEmpty() = " + lTestGoals.isEmpty());
      System.out.println("lPathCounter = " + lPathCounter);
      System.out.println("lPathMaxLength = " + lPathMaxLength);
      
      }
      else {
        // ART has reached a fixpoint, no remaining test goal is reachable
        lInfeasibleTestGoals.addAll(lTestGoals);
        lTestGoals.clear();
      }
      
      if (lTestGoals.isEmpty()) {
        if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {
          outputAbstractReachabilityTree("art_final_", lInitialElements, lAbstractReachabilityTree);
        }
      }
    }
    
    System.out.println("FEASIBLE PATHS");
    System.out.println("lPaths: " + lPaths.size());
    System.out.println("lPathTree: " + lPathTree.getMaximalPaths().size());
    System.out.println("Infeasible test goals (#" + lInfeasibleTestGoals.size() + ") = " + lInfeasibleTestGoals);
    
   
    for (List<AbstractElementWithLocation> p : lPaths) {
      List<String> strpath = AbstractPathToCTranslator.translatePath(pCfas, AbstractPathToCTranslator.getPath(p, null));
      String lFunctionName = p.get(0).getLocationNode().getFunctionName(); 
      //assert(CProver.isFeasible(p.get(0).getLocationNode().getFunctionName(), strpath));
      FShell.isFeasible(strpath, lFunctionName + "_0");
    }
    
    System.out.println("#Test cases computed: " + lPaths.size());
    
    return null;
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
