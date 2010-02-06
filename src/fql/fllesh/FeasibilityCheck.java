package fql.fllesh;

import java.util.LinkedList;
import java.util.Set;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;

import compositeCPA.CompositeCPA;
import compositeCPA.CompositeElement;
import cpa.alwaystop.AlwaysTopCPA;
import cpa.alwaystop.AlwaysTopTopElement;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.concrete.ConcreteAnalysisCPA;
import cpa.concrete.ConcreteAnalysisTopElement;
import cpa.location.LocationCPA;
import cpa.location.LocationElement;
import cpa.mustmay.MustMayAnalysisCPA;
import cpa.mustmay.MustMayAnalysisElement;

import exceptions.CPAException;
import fql.backend.pathmonitor.Automaton;
import fql.backend.targetgraph.Node;
import fql.fllesh.reachability.Query;
import fql.fllesh.reachability.SingletonQuery;
import fql.fllesh.reachability.StandardQuery;
import fql.fllesh.reachability.Waypoint;

public class FeasibilityCheck {
  
  private AlwaysTopCPA mMayCPA;
  private ConcreteAnalysisCPA mMustCPA;
  private MustMayAnalysisCPA mMustMayAnalysisCPA;
  private LocationCPA mLocationCPA;
  private CompositeCPA mCompositeCPA;
  
  public FeasibilityCheck(CFAFunctionDefinitionNode pNode) {
    assert(pNode != null);
    
    mMayCPA = new AlwaysTopCPA();
    mMustCPA = new ConcreteAnalysisCPA();
    
    mMustMayAnalysisCPA = new MustMayAnalysisCPA(mMustCPA, mMayCPA);
    
    try {
      // TODO: check why LocationCPA has a "throws CPAException" clause
      mLocationCPA = new LocationCPA("", "");
    } catch (CPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      
      System.exit(1);
    }
    
    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();
    
    lCPAs.add(mLocationCPA);
    lCPAs.add(mMustMayAnalysisCPA);
    
    mCompositeCPA = CompositeCPA.createNewCompositeCPA(lCPAs, pNode);
  }
  
  public Witness run(LinkedList<Automaton> pAutomatonSequence, LinkedList<Node> pWaypointSequence, Automaton pPassingMonitor, Node pInitialState) {
    
    assert(pAutomatonSequence != null);
    assert(pWaypointSequence != null);
    assert(pPassingMonitor != null);
    assert(pInitialState != null);
    assert(pAutomatonSequence.size() == pWaypointSequence.size());
    
    
    // TODO remove output
    System.out.println(pAutomatonSequence);
    System.out.println(pWaypointSequence);
    
    
    LinkedList<Query> lQueries = new LinkedList<Query>();
    LinkedList<Waypoint> lWaypoints = new LinkedList<Waypoint>();
    
    int lMaxIndex = 0;
    
    int lLastIndex = pAutomatonSequence.size() + 1;
    
    // TODO: brauchen wir auch eine initial precision?
    
    CompositeElement lInitialElement = createInitialElement(pInitialState);
    Automaton lFirstAutomaton = pAutomatonSequence.getFirst();
    Query lInitialQuery = SingletonQuery.create(lInitialElement, lFirstAutomaton, lFirstAutomaton.getInitialStates(), pPassingMonitor, pPassingMonitor.getInitialStates());
    lQueries.add(lInitialQuery);
    
    while (!lQueries.isEmpty()) {
      Query lQuery = lQueries.getLast();
      
      if (lQuery.hasNext()) {
        Waypoint lWaypoint = lQuery.next();
        
        lWaypoints.addLast(lWaypoint);
        
        if (lQueries.size() == lLastIndex) {
          return generateWitness(lWaypoints);
        }
        else {
          
          // check and update backtrack level
          if (lQueries.size() > lMaxIndex) {
            lMaxIndex = lQueries.size();
          }
          
          Set<Integer> lFinalStates;
          
          if (lQueries.size() + 1 == lLastIndex) {
            lFinalStates = pPassingMonitor.getFinalStates();
          }
          else {
            lFinalStates = pPassingMonitor.getStates();
          }
          
          int lQueryIndex = lQueries.size() - 1;
          
          CompositeElement lNextElement = createNextElement(pWaypointSequence.get(lQueryIndex));
          
          Automaton lNextAutomaton = pAutomatonSequence.get(lQueryIndex);
          
          Query lNextQuery = StandardQuery.create(lNextAutomaton, pPassingMonitor, lWaypoint.getElement(), lNextAutomaton.getInitialStates(), lWaypoint.getStatesOfSecondAutomaton(), lNextElement, lNextAutomaton.getFinalStates(), lFinalStates);
          
          lQueries.addLast(lNextQuery);
        }

      }
      else {
        lQueries.removeLast();        
      }
    }
    
    return new InfeasibilityWitness(lMaxIndex);
  }
  
  private CompositeElement createInitialElement(Node pInitialNode) {
    assert(pInitialNode != null);
    
    if (!pInitialNode.getPredicates().isEmpty()) {
      // TODO implement support for predicates, i.e., the initial elements have to be
      // restricted according to the predicates in pInitialState.getPredicates()
      throw new UnsupportedOperationException("Predicates not supported currently!");
    }
    
    CFANode lInitialCFANode = pInitialNode.getCFANode();
    
    AlwaysTopTopElement lAlwaysTopTopElement = AlwaysTopTopElement.getInstance();
    ConcreteAnalysisTopElement lConcreteAnalysisTopElement = ConcreteAnalysisTopElement.getInstance();
    MustMayAnalysisElement lInitialMustMayAnalysisElement = new MustMayAnalysisElement(lConcreteAnalysisTopElement, lAlwaysTopTopElement);
    
    LocationElement lInitialLocationElement = new LocationElement(lInitialCFANode);
    
    LinkedList<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();
    
    lAbstractElements.add(lInitialLocationElement);
    lAbstractElements.add(lInitialMustMayAnalysisElement);
    
    CompositeElement lInitialCompositeElement = new CompositeElement(lAbstractElements, null);   
    
    CallElement lInitialCallElement = new CallElement(lInitialCFANode.getFunctionName(), lInitialCFANode, lInitialCompositeElement);
    
    CallStack lInitialCallStack = new CallStack();
    lInitialCallStack.push(lInitialCallElement);
    lInitialCompositeElement.setCallStack(lInitialCallStack);
    
    return lInitialCompositeElement;
  }
  
  private CompositeElement createNextElement(Node pNextNode) {
    assert(pNextNode != null);
    
    if (!pNextNode.getPredicates().isEmpty()) {
      // TODO implement support for predicates, i.e., the initial elements have to be
      // restricted according to the predicates in pInitialState.getPredicates()
      throw new UnsupportedOperationException("Predicates not supported currently!");
    }
    
    CFANode lCFANode = pNextNode.getCFANode();
    
    AlwaysTopTopElement lAlwaysTopTopElement = AlwaysTopTopElement.getInstance();
    ConcreteAnalysisTopElement lConcreteAnalysisTopElement = ConcreteAnalysisTopElement.getInstance();
    MustMayAnalysisElement lInitialMustMayAnalysisElement = new MustMayAnalysisElement(lConcreteAnalysisTopElement, lAlwaysTopTopElement);
    
    LocationElement lInitialLocationElement = new LocationElement(lCFANode);
    
    LinkedList<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();
    
    lAbstractElements.add(lInitialLocationElement);
    lAbstractElements.add(lInitialMustMayAnalysisElement);
    
    CompositeElement lNextElement = new CompositeElement(lAbstractElements, null);   
    
    // TODO: special treatment of last element? ... stack only containing call to main function
    // ... stack predicates? ... would make sense
    
    //CallElement lInitialCallElement = new CallElement(lInitialCFANode.getFunctionName(), lInitialCFANode, lInitialCompositeElement);
    //CallStack lInitialCallStack = new CallStack();
    //lInitialCallStack.push(lInitialCallElement);
    //lInitialCompositeElement.setCallStack(lInitialCallStack);
    
    return lNextElement;
  }
  
  private FeasibilityWitness generateWitness(LinkedList<Waypoint> lWaypoints) {
    assert(lWaypoints != null);
    
    // TODO implement
    
    return new FeasibilityWitness();
  }
}
