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

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.Pair;
import compositeCPA.CompositeCPA;
import compositeCPA.CompositePrecision;

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
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.common.interfaces.Precision;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpa.symbpredabs.explicit.ExplicitCPA;
import cpa.symbpredabs.explicit.ExplicitTransferRelation;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;
import exceptions.CPAException;
import exceptions.RefinementNeededException;

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
    private CompositeCPA mCompositeCPA;
    private AutomatonCPADomain<CFAEdge> mAutomatonDomain;
    
    public WrapperCPA(CompositeCPA pCompositeCPA, AutomatonCPADomain<CFAEdge> pAutomatonDomain) {
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
    Automaton<CFAEdge> lScopeRestrictionAutomaton = getScopeRestrictionAutomaton();
    ScopeRestrictionCPA lScopeRestrictionCPA = new ScopeRestrictionCPA(lScopeRestrictionAutomaton);
    cpas.add(lScopeRestrictionCPA);

    // initialize symbolic predicate abstraction
    ExplicitCPA lExplicitAbstractionCPA = new ExplicitCPA("sep", "sep");
    cpas.add(lExplicitAbstractionCPA);
    
    ExplicitAbstractFormulaManager lEAFManager = lExplicitAbstractionCPA.getAbstractFormulaManager();
    SymbolicFormulaManager lSFManager = lExplicitAbstractionCPA.getFormulaManager();
    
    
    // get test goal automaton
    Automaton<CFAEdge> lTestGoalAutomaton = getTestGoalAutomaton();
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
      
      lTestGoalCPA = new TestGoalCPA(lSimplifiedAutomaton);
      cpas.add(lTestGoalCPA);
      
      // create composite cpa
      CompositeCPA cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
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
        for (AbstractElement lElement : lReachedElements) {
          System.out.println(lElement);
        }
      } catch (CPAException e1) {
        e1.printStackTrace();
        
        // end test case generation
        break;
      }
      
      // TODO Remove this output
      System.out.print("Infeasible Test Goals: ");
      
      System.out.print("[");
      
      for (Automaton<CFAEdge>.State lState : lTestGoalPrecision.getRemainingFinalStates()) {
        System.out.print(lState);
        System.out.print(" ");
      }
      
      System.out.println("]");
      
      // Remove the infeasible test goals. If the set of remaining final states is
      // not empty this means that we have fully traversed an overapproximation
      // of the reachable state space. This shows that the remaing goals are not
      // reachable at all.
      lTestGoals.removeAll(lTestGoalPrecision.getRemainingFinalStates());
      
      for (AbstractElement lElement : lReachedElements) {
        // are there any remaining test goals to be covered?
        if (lTestGoals.isEmpty()) {
          // we are done, every test goal is reached
          break;
        }
        
        if (lWrapperCPA.getAbstractDomain().getBottomElement().equals(lElement)) {
          continue;
        }
        
        CompositeElement lCompositeElement = (CompositeElement)lElement;
          
        AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
        
        assert(!lTestGoalCPA.getAbstractDomain().getBottomElement().equals(lTmpElement));
        
        // TODO: Why is there a isBottomElement but not a isTopElement?
        // is isBottomElement superfluous?
        if (lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTmpElement)) {
          // TODO: How to handle this, this element should never occur?
          // Should we consider it as covered every test goal?
          continue;
        }
        
        // now, we know it is an StateSetElement
        AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lTmpElement);
        
        final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
        
        boolean lHasFinalStates = false;
        
        // remove all covered test goals
        for (Automaton<CFAEdge>.State lState : lStates) {
          // is lState a remaining test goal?
          if (lState.isFinal()) {
            lHasFinalStates = true;
            
            if (lTestGoals.contains(lState)) {
              // TODO: Remove this output
              System.out.println("=> " + lElement.toString());

              Deque<ExplicitAbstractElement> lPath =
                                                     getAbstractPath((ExplicitAbstractElement) lCompositeElement
                                                         .get(mAbstractionCPAIndex));

              CounterexampleTraceInfo lInfo =
                                              lEAFManager
                                                  .buildCounterexampleTrace(
                                                      lSFManager, lPath);

              if (lInfo.isSpurious()) {
                // TODO: Remove this output
                System.out.println("Path is infeasible");

                TransferRelation lTransferRelation =
                                                     lExplicitAbstractionCPA
                                                         .getTransferRelation();

                ExplicitTransferRelation lExplicitTransferRelation =
                                                                     (ExplicitTransferRelation) lTransferRelation;

                try {
                  lExplicitTransferRelation.performRefinement(lPath, lInfo);
                } catch (RefinementNeededException e) {
                  // TODO: Remove this output
                  System.out.println("Refinement done!");
                } catch (Exception e) {
                  e.printStackTrace();

                  System.exit(1);
                }
              } else {
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
          }
        }
        
        if (!lHasFinalStates) {
          // Because lReached is sorted according to the cardinality of final states
          // we will not see any final states in lReachedElements and thus can stop.
          break;
        }
        
      }
      
      // TODO: invoke CBMC
      //runCBMC(paths);
    }
    
    return lPaths;
  }
}
