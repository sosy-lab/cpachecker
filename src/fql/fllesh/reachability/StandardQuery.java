package fql.fllesh.reachability;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import cfa.objectmodel.CFANode;

import compositeCPA.CompositeElement;
import compositeCPA.CompositePrecision;

import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.ReachedElements.TraversalMethod;
import fql.backend.pathmonitor.Automaton;
import fql.fllesh.cpa.QueryStandardElement;

public class StandardQuery extends AbstractQuery {

  private LinkedList<Waypoint> lNextWaypoints;
  
  //public static StandardQuery create(Automaton pFirstAutomaton, Automaton pSecondAutomaton, CompositeElement pSourceElement, CompositePrecision pSourcePrecision, Set<Integer> pSourceStatesOfFirstAutomaton, Set<Integer> pSourceStatesOfSecondAutomaton, CompositeElement pTargetElement, Set<Integer> pTargetStatesOfFirstAutomaton, Set<Integer> pTargetStatesOfSecondAutomaton) {
  public static StandardQuery create(Automaton pFirstAutomaton, Automaton pSecondAutomaton, CompositeElement pSourceElement, CompositePrecision pSourcePrecision, Set<Integer> pSourceStatesOfFirstAutomaton, Set<Integer> pSourceStatesOfSecondAutomaton, CFANode pCFANode, Set<Integer> pTargetStatesOfFirstAutomaton, Set<Integer> pTargetStatesOfSecondAutomaton) {
    StandardQuery lQuery = new StandardQuery(pFirstAutomaton, pSecondAutomaton);
    
    Waypoint lSource = new Waypoint(lQuery, pSourceElement, pSourcePrecision, pSourceStatesOfFirstAutomaton, pSourceStatesOfSecondAutomaton);
    //Waypoint lTarget = new Waypoint(lQuery, pTargetElement, null, pTargetStatesOfFirstAutomaton, pTargetStatesOfSecondAutomaton);
    
    // TODO support for predicates is missing
    // TODO support for call stack missing
    TargetPoint lTarget = new TargetPoint(pCFANode, pFirstAutomaton, pTargetStatesOfFirstAutomaton, pSecondAutomaton, pTargetStatesOfSecondAutomaton);
    
    lQuery.mSource = lSource;
    lQuery.mTarget = lTarget;
    
    return lQuery;
  }
  
  private Waypoint mSource;
  //private Waypoint mTarget;
  private TargetPoint mTarget;
  
  /*private AlwaysTopCPA mMayCPA;
  private ConcreteAnalysisCPA mMustCPA;
  private MustMayAnalysisCPA mMustMayAnalysisCPA;
  private LocationCPA mLocationCPA;
  private CompositeCPA mCompositeCPA;
  private QueryCPA mQueryCPA;*/
  
  private boolean mExplorationFinished;
  private ReachedElements mReachedElements;
  
  private StandardQuery(Automaton pFirstAutomaton, Automaton pSecondAutomaton) {
    super(pFirstAutomaton, pSecondAutomaton);
    
    lNextWaypoints = new LinkedList<Waypoint>();
    
    // create CPA
    // TODO: we need a different CPA than given here
    // LocationCPA, QueryCPA
    /*mMayCPA = new AlwaysTopCPA();
    mMustCPA = new ConcreteAnalysisCPA();
    
    mMustMayAnalysisCPA = new MustMayAnalysisCPA(mMustCPA, mMayCPA);
    
    mQueryCPA = new QueryCPA(this, mMustMayAnalysisCPA);
    
    mLocationCPA = new LocationCPA("", "");
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(mLocationCPA);
    lCPAs.add(mQueryCPA);
    
    // TODO: Problem: mSource and mTarget are initialized after calling the constructor!!!
    
    // creation of initial element and precision
    // TODO: wrong initial element ... problem: set of initial elements
    CompositeElement lInitialElement = getSource().getElement();
    // TODO: wrong initial precision ... maybe this precision is even correct ... check transition relation
    CompositePrecision lInitialPrecision = getSource().getPrecision();
        
    
    
    mCompositeCPA = CompositeCPA.createNewCompositeCPA(lCPAs, lInitialElement, lInitialPrecision);
    
    mExplorationFinished = false;*/
    
    // TODO what about other traversal types?
    mReachedElements = new ReachedElements(TraversalMethod.DFS, true);
  }
  
  public Waypoint getSource() {
    return mSource;
  }
  
  //public Waypoint getTarget() {
  public TargetPoint getTarget() {
    return mTarget;
  }
  
  private void explore() {
    
    if (mReachedElements.isEmpty()) {
      // initialization
      // TODO implement
      
      // apply state space exploration
      // TODO implement
      
    }
    else {
      // refinement necessary 
      // TODO implement
      
      // apply state space exploration
      // TODO implement
      
    }
    
    // evaluate reached elements with regard to new waypoints
    // Here, the initial state can be treated as a waypoint, too. 
    // Do we need a set with already seen waypoints to exclude 
    // reinvestigating waypoints?
    
    
    // TODO derive from composite element in order to introduce a structure
    
    
    //Set<AbstractElement> lPotentialTargets = mReachedElements.getReached(mTarget.getElement());
    // TODO support for predicates is missing
    Set<AbstractElement> lPotentialTargets = mReachedElements.getReached(mTarget.getCFANode());
    
    //CallStack lTargetCallStack = mTarget.getElement().getCallStack();
    
    Set<QueryStandardElement> lPotentialTargetsForRefinement = new HashSet<QueryStandardElement>();
    Set<QueryStandardElement> lDefiniteTargets = new HashSet<QueryStandardElement>();
    
    for (AbstractElement lPotentialTarget : lPotentialTargets) {
      CompositeElement lElement = (CompositeElement)lPotentialTarget;
      
      QueryStandardElement lQueryElement = lElement.retrieveWrappedElement(QueryStandardElement.class);
      
      assert(lQueryElement != null);
      
      if (mTarget.satisfiesTarget(lQueryElement)) {
        if (lQueryElement.getMustState1() && lQueryElement.getMustState2()) {
          lDefiniteTargets.add(lQueryElement);
        }
        else {
          lPotentialTargetsForRefinement.add(lQueryElement);
        }
      }
    }
    
    
    // TODO process targets and potential targets for refinement and add to lNextWaypoints
    System.out.println("Definite Targets: " + lDefiniteTargets);
    System.out.println("Potential Targets: " + lPotentialTargetsForRefinement);
    
    
    if (lNextWaypoints.isEmpty()) {
      // we have not found any new waypoints
      mExplorationFinished = true;
    }
  }
  
  @Override
  public boolean hasNext() {
    if (mExplorationFinished && lNextWaypoints.isEmpty()) {
      return false;
    }
    else {
      if (lNextWaypoints.isEmpty()) {
        
        explore();
        
        return hasNext();        
      }
      else {
        return true;
      }
    }
  }

  @Override
  public Waypoint next() {
    
    if (lNextWaypoints.isEmpty()) {
      if (mExplorationFinished) {
        throw new NoSuchElementException();
      }
      
      explore();
      
      return next();
    }
    else {
      return lNextWaypoints.removeFirst();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove is not supported!");
  }

}
