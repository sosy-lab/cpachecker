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
import cfa.objectmodel.CFAFunctionDefinitionNode;

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
    
    
    while (!lTestGoals.isEmpty()) {
      // TODO remove this output
      System.out.println("NEXT LOOP (" + (lLoopCounter++) + ") #####################");
      

      // Automaton simplification prevents exponentational blow up with respect
      // to stop operator!
      // TODO simplify automaton
      
      
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
      
      try {
        algo.CPAWithInitialSet(cpa, lInitialElements, lInitialPrecision);
        
        lInitialElements.clear();
      } catch (CPAException e1) {
        e1.printStackTrace();
        assert(false);
      }
      
      
      
      // TODO Remove this output
      printTestGoals("Infeasible Test Goals: ", lTestGoalPrecision.getRemainingFinalStates());
      
      lInfeasibleTestGoals.addAll(lTestGoalPrecision.getRemainingFinalStates());
      
      // Remove the infeasible test goals. If the set of remaining final states is
      // not empty this means that we have fully traversed an overapproximation
      // of the reachable state space. This shows that the remaing goals are not
      // reachable at all.
      lTestGoals.removeAll(lTestGoalPrecision.getRemainingFinalStates());
      
      
      
      // process abstract reachability tree
      ParametricAbstractReachabilityTree<CompositeElement> lAbstractReachabilityTree = cpa.getTransferRelation().getAbstractReachabilityTree();
      
      System.out.println("Size of ART: " + lAbstractReachabilityTree.size());
      
      
      
      if (CPAMain.cpaConfig.getBooleanValue("art.visualize")) {      
        File lFile = null;

        try {
          lFile = File.createTempFile("art_" + lLoopCounter + "_", ".dot");
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

        lWriter.println(lAbstractReachabilityTree.toDot(lOldInitialElements));

        lWriter.close();

        try {
          File lPostscriptFile = File.createTempFile("art_" + lLoopCounter + "_", ".ps");

          lPostscriptFile.deleteOnExit();

          Process lDotProcess = Runtime.getRuntime().exec("dot -Tps -o" + lPostscriptFile.getAbsolutePath() + " " + lFile.getAbsolutePath());

          lDotProcess.waitFor();

          File lPDFFile = File.createTempFile("art_" + lLoopCounter + "_", ".pdf");

          Process lPs2PdfProcess = Runtime.getRuntime().exec("ps2pdf " + lPostscriptFile.getAbsolutePath() + " " + lPDFFile.getAbsolutePath());

          lPs2PdfProcess.waitFor();
        }
        catch (Exception e) {
          e.printStackTrace();
          assert(false);
        }
      }
      
      
      
      assert(lAbstractReachabilityTree.hasRoot());
      
      CompositeElement lRoot = lAbstractReachabilityTree.getRoot();
      
      System.out.println("ROOT " + lRoot);
      
      Stack<Pair<CompositeElement, Iterator<CompositeElement>>> lStack = new Stack<Pair<CompositeElement, Iterator<CompositeElement>>>();
      
      assert(!lAbstractReachabilityTree.getChildren(lRoot).isEmpty());
      
      lStack.push(new Pair(lRoot, lAbstractReachabilityTree.getChildren(lRoot).iterator()));
      
      
      
      int lPathCounter = 0;
      int lPathMaxLength = 0;

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
            // we are at a leave
            

            lPathCounter++;
            
            System.out.println(lPathCounter);
            System.out.flush();
            
            if (lStack.size() > lPathMaxLength) {
              lPathMaxLength = lStack.size();
            }

            AbstractElement lTmpElement = lChild.get(mTestGoalCPAIndex);

            // the ART should not contain the bottom element
            assert (!lTestGoalCPA.getAbstractDomain().isBottomElement(lTmpElement));

            // top element should never occur
            assert (!lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTmpElement));

            // now, we know it is an StateSetElement
            //AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lTmpElement);

            //final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();

            /*boolean lHasUncoveredTestGoal = false;

            for (Automaton<CFAEdge>.State lState : lStates) {
              if (lState.isFinal() && lTestGoals.contains(lState)) {
                lHasUncoveredTestGoal = true;
                break;
              }
            }*/
            
            boolean lHasUncoveredTestGoal = true;

            if (lHasUncoveredTestGoal) {
              
              List<AbstractElementWithLocation> lPath = new ArrayList<AbstractElementWithLocation>(lStack.size() + 1);
              
              for (Pair<CompositeElement, Iterator<CompositeElement>> lStackElement : lStack) {
                lPath.add(lStackElement.getFirst());
              }
              
              lPath.add(lChild);
              
              
              boolean lFeasible = false;
              
              CompositeElement lInfeasibleElement = null;
              
              do {
                List<CFAEdge> lCFAPath = AbstractPathToCTranslator.getPath(lPath);
              
                List<String> lPathStringRepresentation = AbstractPathToCTranslator.translatePath(pCfas, lCFAPath);
                
                lFeasible = CProver.isFeasible(lPath.get(0).getLocationNode().getFunctionName(), lPathStringRepresentation);
                
                if (!lFeasible) {
                  lInfeasibleElement = (CompositeElement)lPath.remove(lPath.size() - 1);
                }
              }
              while (!lFeasible);
              
              assert(lPath.size() > 0);
              
              CompositeElement lLastFeasibleElement = (CompositeElement)lPath.get(lPath.size() - 1);
              
              // + 1 comes from lChild that is NOT on the stack!
              if (lPath.size() == lStack.size() + 1) {
                //System.out.println("===> " + lLastFeasibleElement);
                
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

              // remove the test goal from the automaton
              // BY DOING THIS WE GET AN EXPONENTIAL BLOW-UP
              /*for (Automaton<CFAEdge>.State lState : lBacktrackStates) {
                lState.unsetFinal();
              }*/
              
              // backtrack
              while (lStack.size() > lPath.size()) {
                lStack.pop();
              }
              
              // cleanup ART
              if (lInfeasibleElement != null) {
                lAbstractReachabilityTree.removeSubtree(lInfeasibleElement);
                
                //lAbstractReachabilityTree.getChildren((CompositeElement)lPath.get(lPath.size() - 1)).remove(lInfeasibleElement);
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
      
      /*for (CompositeElement lStoppedElement : cpa.getStopOperator().getStoppedElements()) {
        if (!cpa.getAbstractDomain().isBottomElement(lStoppedElement)) {
          if (lAbstractReachabilityTree.contains(lStoppedElement)) {
            lInitialElements.add(lStoppedElement);
          }
        }
      }*/
      
      //System.out.println(lInitialElements.size() + " initial elements");
      
      System.out.println();
      System.out.println("lPathCounter = " + lPathCounter);
      System.out.println("lPathMaxLength = " + lPathMaxLength);
    }
    
    printTestGoals("Infeasible test goals: ", lInfeasibleTestGoals);
    
    return null;
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
