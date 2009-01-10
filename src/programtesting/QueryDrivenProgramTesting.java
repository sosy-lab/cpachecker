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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

import common.Pair;
import compositeCPA.CompositeMergeOperator;
import compositeCPA.CompositePrecision;
import compositeCPA.CompositePrecisionAdjustment;
import compositeCPA.CompositeStopOperator;

import cpa.common.CPAAlgorithm;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.common.interfaces.Precision;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.UpdateablePredicateMap;
import cpa.symbpredabs.explicit.BDDMathsatExplicitAbstractManager;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpa.symbpredabs.explicit.ExplicitCPA;
import cpa.symbpredabs.explicit.ExplicitTransferRelation;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;
import exceptions.CPAException;
import exceptions.CPATransferException;
import java.util.Stack;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {

  
  public static class MyCollection implements Collection<Pair<AbstractElementWithLocation,Precision>> {
    class MyIterator implements Iterator<Pair<AbstractElementWithLocation,Precision>> {
      private Iterator<Pair<AbstractElementWithLocation,Precision>> lInnerIterator;
      
      private int lMapIndex;
      
      public MyIterator() {
        lMapIndex = TOP_INDEX;
        lInnerIterator = null;
      }
      
      private void init() {
        if (lInnerIterator == null) {
          lInnerIterator = mMap.get(lMapIndex).iterator();
        }
      }
      
      @Override
      public boolean hasNext() {
        init();
        
        if (lInnerIterator.hasNext()) {
          return true;
        }
        
        if (lMapIndex > BOTTOM_INDEX) {
          lMapIndex--;
          lInnerIterator = mMap.get(lMapIndex).iterator();
          
          return hasNext();
        }
        
        return false;
      }

      @Override
      public Pair<AbstractElementWithLocation,Precision> next() {
        init();
        
        if (hasNext()) {
          return lInnerIterator.next();
        }

        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        init();
        
        lInnerIterator.remove();
      }
      
    }
    
    private Map<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> mMap;
    private AutomatonCPADomain<CFAEdge> mAutomatonDomain;
    
    private final int TOP_INDEX;
    private final int BOTTOM_INDEX;
    
    public MyCollection(AutomatonCPADomain<CFAEdge> pAutomatonDomain) {
      assert(pAutomatonDomain != null);
      
      mAutomatonDomain = pAutomatonDomain;
      
      mMap = new HashMap<Integer, Set<Pair<AbstractElementWithLocation,Precision>>>();
      
      // top
      assert(pAutomatonDomain.getAutomaton().getFinalStates().size() < Integer.MAX_VALUE);
      TOP_INDEX = pAutomatonDomain.getAutomaton().getFinalStates().size() + 1;
      mMap.put(TOP_INDEX, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
      
      for (int i = 0; i <= pAutomatonDomain.getAutomaton().getFinalStates().size(); i++) {
        mMap.put(i, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
      }
      
      // bottom
      BOTTOM_INDEX = -1;
      mMap.put(BOTTOM_INDEX, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
    }
    
    @Override
    public boolean add(Pair<AbstractElementWithLocation,Precision> pE) {
      assert(pE != null);
      assert(pE.getFirst() != null);
      assert(pE.getFirst() instanceof CompositeElement);
      
      CompositeElement lCompositeElement = (CompositeElement)pE.getFirst();
      
      AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);
        
        assert(lSet != null);
        
        return lSet.add(pE);
      }
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);
        
        assert(lSet != null);
        
        return lSet.add(pE);
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);
      
      final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
      
      int lNumberOfFinalStates = 0;
      
      for (Automaton<CFAEdge>.State lState : lStates) {
        if (lState.isFinal()) {
          lNumberOfFinalStates++;
        }
      }
      
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);
      
      assert(lSet != null);
      
      return lSet.add(pE);
    }

    @Override
    public boolean addAll(Collection<? extends Pair<AbstractElementWithLocation,Precision>> pC) {
      assert(pC != null);
      
      boolean lWasChanged = false;
      
      for (Pair<AbstractElementWithLocation,Precision> lElement : pC) {
        if (add(lElement)) {
          lWasChanged = true;
        }
      }
      
      return lWasChanged;
    }

    @Override
    public void clear() {
      for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
        lEntry.getValue().clear();
      }
    }

    @Override
    public boolean contains(Object pO) {
      assert(pO != null);
      assert(pO instanceof Pair<?,?>);
      
      Pair<AbstractElementWithLocation,Precision> lPair = (Pair<AbstractElementWithLocation,Precision>)pO;
      
      assert(lPair.getFirst() instanceof CompositeElement);
      
      CompositeElement lCompositeElement = (CompositeElement)lPair.getFirst();
      
      AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);
        
        assert(lSet != null);
        
        return lSet.contains(pO);
      }
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);
        
        assert(lSet != null);
        
        return lSet.contains(pO);
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);
      
      final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
      
      int lNumberOfFinalStates = 0;
      
      for (Automaton<CFAEdge>.State lState : lStates) {
        if (lState.isFinal()) {
          lNumberOfFinalStates++;
        }
      }
      
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);
      
      assert(lSet != null);
      
      return lSet.contains(pO);
    }

    @Override
    public boolean containsAll(Collection<?> pC) {
      assert(pC != null);
      
      for (Object lObject : pC) {
        if (!contains(lObject)) {
          return false;
        }
      }
      
      return true;
    }

    @Override
    public boolean isEmpty() {
      for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
        if (!lEntry.getValue().isEmpty()) {
          return false;
        }
      }
      
      return true;
    }

    @Override
    public Iterator<Pair<AbstractElementWithLocation,Precision>> iterator() {
      return new MyIterator();
    }

    @Override
    public boolean remove(Object pO) {
      assert(pO != null);
      
      assert(pO instanceof CompositeElement);
      
      CompositeElement lCompositeElement = (CompositeElement)pO;
      
      AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);
        
        assert(lSet != null);
        
        return lSet.remove(pO);
      }
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);
        
        assert(lSet != null);
        
        return lSet.remove(pO);
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);
      
      final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
      
      int lNumberOfFinalStates = 0;
      
      for (Automaton<CFAEdge>.State lState : lStates) {
        if (lState.isFinal()) {
          lNumberOfFinalStates++;
        }
      }
      
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);
      
      assert(lSet != null);
      
      return lSet.remove(pO);
    }

    @Override
    public boolean removeAll(Collection<?> pC) {
      assert(pC != null);
      
      boolean lWasChanged = false;
      
      for (Object lObject : pC) {
        if (remove(lObject)) {
          lWasChanged = true;
        }
      }
      
      return lWasChanged;
    }

    @Override
    public boolean retainAll(Collection<?> pC) {
      assert(pC != null);
      
      boolean lWasChanged = false;
      
      for (Pair<AbstractElementWithLocation,Precision> lElement : this) {
        if (!pC.contains(lElement)) {
          if (remove(lElement)) {
            lWasChanged = true;
          }
        }
      }
      
      return lWasChanged;
    }

    @Override
    public int size() {
      int lSize = 0;
      
      for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
        lSize += lEntry.getValue().size();
      }
      
      return lSize;
    }

    @Override
    public Object[] toArray() {
      int lSize = size();
      
      if (lSize == 0) {
        return new Object[0];
      }
      
      Object[] lObjects = new Object[lSize];
      
      int lIndex = 0;
      
      for (Object lObject : this) {
        lObjects[lIndex] = lObject;
        lIndex++;
      }
      
      return lObjects;
    }

    @Override
    public <T> T[] toArray(T[] pA) {
      assert(pA != null);
      
      int lSize = size();
      
      if (lSize <= pA.length) {
        int lIndex = 0;
        
        for (Object lObject : this) {
          pA[lIndex] = (T)lObject;
        }
        
        for (int i = lSize; i < pA.length; i++) {
          pA[i] = null;
        }
        
        return pA;
      }
      else {
        return (T[])toArray();
      }
    }
    
    @Override
    public String toString() {
      return mMap.toString();
    }
  }
  
  public static class WrapperCPA implements ConfigurableProgramAnalysis {
    private QDPTCompositeCPA mCompositeCPA;
    private AutomatonCPADomain<CFAEdge> mAutomatonDomain;
    
    public WrapperCPA(QDPTCompositeCPA pCompositeCPA, AutomatonCPADomain<CFAEdge> pAutomatonDomain) {
      assert(pCompositeCPA != null);
      assert(pAutomatonDomain != null);
      
      mCompositeCPA = pCompositeCPA;
      mAutomatonDomain = pAutomatonDomain;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
      return mCompositeCPA.getAbstractDomain();
    }

    @Override
    public <AE extends AbstractElement> AE getInitialElement(CFAFunctionDefinitionNode pNode) {
      return (AE) mCompositeCPA.getInitialElement(pNode);
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
    public Collection<Pair<AbstractElementWithLocation,Precision>> newReachedSet() {
      return new MyCollection(mAutomatonDomain);
    }

    @Override
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return mCompositeCPA.getInitialPrecision(pNode);
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return mCompositeCPA.getPrecisionAdjustment();
    }
  }
  
  public static Deque<ExplicitAbstractElement> getAbstractPath(ExplicitAbstractElement pElement) {
    ExplicitAbstractElement lPathElement = pElement;
    
    Deque<ExplicitAbstractElement> lPath = new LinkedList<ExplicitAbstractElement>();
    
    while (lPathElement != null) {
      lPath.addFirst(lPathElement);
      
      lPathElement = lPathElement.getParent();
    }
    
    return lPath;
  }
  
  //private final static int mLocationCPAIndex = 0;
  //private final static int mScopeRestrictionCPAIndex = 1;
  private final static int mTestGoalCPAIndex = 3;
  private final static int mAbstractionCPAIndex = 2;
  
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
    Automaton<CFAEdge> lScopeRestrictionAutomaton = AutomatonTestCases.getScopeRestrictionAutomaton(pMainFunction);
    ScopeRestrictionCPA lScopeRestrictionCPA = new ScopeRestrictionCPA(lScopeRestrictionAutomaton);
    cpas.add(lScopeRestrictionCPA);

    // initialize symbolic predicate abstraction
    ExplicitCPA lExplicitAbstractionCPA = new ExplicitCPA("sep", "sep");
    cpas.add(lExplicitAbstractionCPA);
    
    ExplicitAbstractFormulaManager lEAFManager = lExplicitAbstractionCPA.getAbstractFormulaManager();
    SymbolicFormulaManager lSFManager = lExplicitAbstractionCPA.getFormulaManager();
    
    
    // get test goal automaton
    Automaton<CFAEdge> lTestGoalAutomaton = AutomatonTestCases.getTestGoalAutomaton(pMainFunction);
    TestGoalCPA lTestGoalCPA = new TestGoalCPA(lTestGoalAutomaton);
    cpas.add(lTestGoalCPA);
    
    
    CPAAlgorithm algo = new CPAAlgorithm();

    // every final state in the test goal automaton represents a 
    // test goal, so initialize test goals with the final states
    // of the test goal automaton
    Set<Automaton<CFAEdge>.State> lTestGoals = lTestGoalAutomaton.getFinalStates();

    // the resulting set of paths
    Set<Deque<ExplicitAbstractElement>> lPaths = new HashSet<Deque<ExplicitAbstractElement>>();
    
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
      QDPTCompositeCPA cpa = new QDPTCompositeCPA(cpas, pMainFunction);
      WrapperCPA lWrapperCPA = new WrapperCPA(cpa, lTestGoalCPA.getAbstractDomain());
      
      
      AbstractElementWithLocation lInitialElement = lWrapperCPA.getInitialElement(pMainFunction);
      Precision lInitialPrecision = lWrapperCPA.getInitialPrecision(pMainFunction);
      
      
      // TODO This is kind of a hack
      CompositePrecision lCompositePrecision = (CompositePrecision)lInitialPrecision;
      TestGoalPrecision lTestGoalPrecision = (TestGoalPrecision)lCompositePrecision.get(mTestGoalCPAIndex);
      // reset precision to test goals
      // TODO Hack
      lTestGoalPrecision.setTestGoals(lTestGoals);
      
      Collection<AbstractElementWithLocation> lReachedElements = null;
      
      try {
        lReachedElements = algo.CPA(lWrapperCPA, lInitialElement, lInitialPrecision);
        
        // TODO: Remove this output
        //printReachedElements(lReachedElements);
      } catch (CPAException e1) {
        e1.printStackTrace();
        
        // end test case generation
        break;
      }
      
      
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
            assert (!lTestGoalCPA.getAbstractDomain().isBottomElement(lTmpElement));

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

              Pair<CounterexampleTraceInfo, Integer> lPair = lMathsatManager.buildCounterexampleTrace2(lSFManager, lPath);

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
                Pair<CounterexampleTraceInfo, Integer> lBacktrackPair = lMathsatManager.buildCounterexampleTrace2(lSFManager, lBacktrackPath);

                CounterexampleTraceInfo lBacktrackInfo = lBacktrackPair.getFirst();

                assert (!lBacktrackInfo.isSpurious());


                AbstractElement lBacktrackTmpElement = lChild.get(mTestGoalCPAIndex);

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
                for (Automaton<CFAEdge>.State lState : lBacktrackStates) {
                  lState.unsetFinal();
                }

                // add feasible path to set of feasible paths
                lPaths.add(lBacktrackPath);
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
    
    Map<Deque<ExplicitAbstractElement>, List<String>> lTranslations = AbstractPathToCTranslator.translatePaths(lPaths);

    for (Entry<Deque<ExplicitAbstractElement>, Boolean> lVerified : CProver.checkSat(lTranslations).entrySet()) {
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
    }
    
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

  public static void printReachedElements(Collection<AbstractElementWithLocation> pReachedElements) {
    System.out.println("reached elements begin");

    for (AbstractElement lElement : pReachedElements) {
      System.out.println(lElement);
    }

    System.out.println("reached elements end");
  }
}
