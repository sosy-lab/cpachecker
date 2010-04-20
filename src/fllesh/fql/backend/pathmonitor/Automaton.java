package fllesh.fql.backend.pathmonitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import fllesh.fql.backend.targetgraph.Edge;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.backend.testgoals.EdgeSequence;
import fllesh.fql.frontend.ast.DefaultASTVisitor;
import fllesh.fql.frontend.ast.filter.BasicBlockEntry;
import fllesh.fql.frontend.ast.filter.Column;
import fllesh.fql.frontend.ast.filter.Complement;
import fllesh.fql.frontend.ast.filter.Compose;
import fllesh.fql.frontend.ast.filter.ConditionEdge;
import fllesh.fql.frontend.ast.filter.ConditionGraph;
import fllesh.fql.frontend.ast.filter.DecisionEdge;
import fllesh.fql.frontend.ast.filter.EnclosingScopes;
import fllesh.fql.frontend.ast.filter.Expression;
import fllesh.fql.frontend.ast.filter.File;
import fllesh.fql.frontend.ast.filter.Filter;
import fllesh.fql.frontend.ast.filter.Function;
import fllesh.fql.frontend.ast.filter.FunctionCall;
import fllesh.fql.frontend.ast.filter.FunctionCalls;
import fllesh.fql.frontend.ast.filter.FunctionEntry;
import fllesh.fql.frontend.ast.filter.FunctionExit;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.filter.Intersection;
import fllesh.fql.frontend.ast.filter.Label;
import fllesh.fql.frontend.ast.filter.Line;
import fllesh.fql.frontend.ast.filter.RegularExpression;
import fllesh.fql.frontend.ast.filter.SetMinus;
import fllesh.fql.frontend.ast.filter.Union;
import fllesh.fql.frontend.ast.pathmonitor.Alternative;
import fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import fllesh.fql.frontend.ast.pathmonitor.PathMonitor;
import fllesh.fql.frontend.ast.pathmonitor.UpperBound;
import fllesh.fql.frontend.ast.predicate.Predicates;

public class Automaton {

  private Set<Integer> mInitialStates;
  private Set<Integer> mFinalStates;
  private DirectedGraph<Integer, AutomatonEdge> mTransitionRelation;
  private int mNextFreeState;
  
  private static Automaton mZeroAutomaton = new Automaton(Collections.singleton(0), Collections.singleton(0), new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class), 1);
  
  private Automaton(Set<Integer> pInitialStates, Set<Integer> pFinalStates, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation, int pNextFreeState) {
    assert(pInitialStates != null);
    assert(pFinalStates != null);
    assert(pTransitionRelation != null);
    
    mInitialStates = pInitialStates;
    mFinalStates = pFinalStates;
    
    mTransitionRelation = pTransitionRelation;
    
    mNextFreeState = pNextFreeState;
  }
  
  public Set<AutomatonEdge> getOutgoingEdges(Integer pState) {
    assert(pState != null);
    
    return mTransitionRelation.outgoingEdgesOf(pState);
  }
  
  public Set<Integer> getInitialStates() {
    return mInitialStates;
  }
  
  public Set<Integer> getFinalStates() {
    return mFinalStates;
  }
  
  public Set<Integer> getStates() {
    return mTransitionRelation.vertexSet();
  }
  
  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    
    lBuffer.append("(Init: ");
    lBuffer.append(mInitialStates.toString());
    lBuffer.append(", Final: ");
    lBuffer.append(mFinalStates.toString());
    lBuffer.append(", ");
    lBuffer.append(mTransitionRelation.edgeSet().toString());
    lBuffer.append(")");
    
    return lBuffer.toString();
  }
  
  public static Automaton create(PathMonitor pMonitor, TargetGraph pTargetGraph) {
    assert(pMonitor != null);
    assert(pTargetGraph != null);
    
    PathMonitorCreator lCreator = new PathMonitorCreator(pTargetGraph);
    
    Automaton lResult = pMonitor.accept(lCreator);
    
    // TODO implement automaton cache
    
    return lResult;
  }
  
  public static Automaton create(Edge pEdge) {
    assert(pEdge != null);
    
    return Automaton.create(Identity.getInstance(), TargetGraph.createTargetGraph(pEdge));
  }
  
  public static Automaton create(EdgeSequence pEdgeSequence) {
    assert(pEdgeSequence != null);
    
    DirectedGraph<Integer, AutomatonEdge> lTransitionRelation = new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class);
    
    int lIndex = 0;
    
    for (Edge lEdge : pEdgeSequence) {
      new TargetGraphEdge(lIndex, lIndex + 1, lTransitionRelation, TargetGraph.createTargetGraph(lEdge));
      lIndex++;
    }

    return new Automaton(Collections.singleton(0), Collections.singleton(lIndex), lTransitionRelation, lIndex + 1);
  }
  
  private static class PathMonitorCreator extends DefaultASTVisitor<Automaton> {

    private TargetGraph mTargetGraph;
    
    public PathMonitorCreator(TargetGraph pTargetGraph) {
      assert(pTargetGraph != null);
      
      mTargetGraph = pTargetGraph;
    }
    
    public Automaton visitFilter(Filter pFilter) {
      assert(pFilter != null);
      
      TargetGraph lFilteredTargetGraph = mTargetGraph.apply(pFilter);
      
      // automaton states
      Integer lZero = 0;
      Integer lOne = 1;
      
      // new transition relation
      DirectedGraph<Integer, AutomatonEdge> lTransitionRelation = new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class);
      
      // new automaton transition
      new TargetGraphEdge(lZero, lOne, lTransitionRelation, lFilteredTargetGraph);
      
      // new automaton
      HashSet<Integer> lInitialStates = new HashSet<Integer>();
      HashSet<Integer> lFinalStates = new HashSet<Integer>();
      
      lInitialStates.add(lZero);
      lFinalStates.add(lOne);
      
      return new Automaton(lInitialStates, lFinalStates, lTransitionRelation, 2);
    }
    
    @Override
    public Automaton visit(Identity pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(File pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(BasicBlockEntry pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(ConditionEdge pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(ConditionGraph pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(DecisionEdge pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Line pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(FunctionCalls pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Column pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Function pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(FunctionCall pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(FunctionEntry pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(FunctionExit pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Label pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Expression pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(RegularExpression pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Complement pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Union pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Compose pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(Intersection pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(SetMinus pFilter) {
      return visitFilter(pFilter);
    }

    @Override
    public Automaton visit(EnclosingScopes pFilter) {
      return visitFilter(pFilter);
    }
    
    @Override
    public Automaton visit(Alternative pAlternative) {
      assert(pAlternative != null);
      
      PathMonitor lLeftMonitor = pAlternative.getLeftSubmonitor();
      PathMonitor lRightMonitor = pAlternative.getRightSubmonitor();
      
      Automaton lLeftAutomaton = lLeftMonitor.accept(this);
      Automaton lRightAutomaton = lRightMonitor.accept(this);
      
      return alternative(lLeftAutomaton, lRightAutomaton);
    }

    @Override
    public Automaton visit(ConditionalMonitor pConditionalMonitor) {
      assert(pConditionalMonitor != null);
      
      Predicates lPreconditions = pConditionalMonitor.getPreconditions();
      Predicates lPostconditions = pConditionalMonitor.getPostconditions();
      
      Automaton lSubautomaton = Automaton.create(pConditionalMonitor.getSubmonitor(), mTargetGraph);
      
      if (lPreconditions.isEmpty() && lPostconditions.isEmpty()) {
        return lSubautomaton;
      }
      
      // duplicate automaton
      DirectedGraph<Integer, AutomatonEdge> lTransitionRelation = new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class);
      
      for (Integer lState : lSubautomaton.mTransitionRelation.vertexSet()) {
        lTransitionRelation.addVertex(lState);
      }
      
      for (AutomatonEdge lEdge : lSubautomaton.mTransitionRelation.edgeSet()) {
        lEdge.duplicate(lEdge.getSource(), lEdge.getTarget(), lTransitionRelation);        
      }
      
      Set<Integer> lInitialStates = new HashSet<Integer>();
      Set<Integer> lFinalStates = new HashSet<Integer>();
      
      int lNextFreeState = lSubautomaton.mNextFreeState;
      
      if (lPreconditions.isEmpty()) {
        lInitialStates.addAll(lSubautomaton.mInitialStates);
      }
      else {
        lInitialStates.add(lNextFreeState);
        
        for (Integer lInitialState : lSubautomaton.mInitialStates) {
          new PredicatesEdge(lNextFreeState, lInitialState, lTransitionRelation, lPreconditions);
        }
        
        lNextFreeState++;
      }
      
      if (lPostconditions.isEmpty()) {
        lFinalStates.addAll(lSubautomaton.mFinalStates);
      }
      else {
        lFinalStates.add(lNextFreeState);
        
        for (Integer lFinalState : lSubautomaton.mFinalStates) {
          new PredicatesEdge(lFinalState, lNextFreeState, lTransitionRelation, lPostconditions);
        }
        
        lNextFreeState++;
      }
      
      return new Automaton(lInitialStates, lFinalStates, lTransitionRelation, lNextFreeState);
    }

    @Override
    public Automaton visit(Concatenation pConcatenation) {
      assert(pConcatenation != null);
      
      PathMonitor lLeftMonitor = pConcatenation.getLeftSubmonitor();
      PathMonitor lRightMonitor = pConcatenation.getRightSubmonitor();
      
      Automaton lLeftAutomaton = lLeftMonitor.accept(this);
      Automaton lRightAutomaton = lRightMonitor.accept(this);
      
      return concatenate(lLeftAutomaton, lRightAutomaton);      
    }

    @Override
    public Automaton visit(LowerBound pLowerBound) {
      assert(pLowerBound != null);
      assert(pLowerBound.getBound() >= 0);
      
      Automaton lSubautomaton = pLowerBound.getSubmonitor().accept(this);
      
      return concatenate(repeatKTimes(lSubautomaton, pLowerBound.getBound()), kleeneStar(lSubautomaton));
    }

    @Override
    public Automaton visit(UpperBound pUpperBound) {
      assert(pUpperBound != null);
      assert(pUpperBound.getBound() >= 0);
      
      Automaton lSubautomaton = pUpperBound.getSubmonitor().accept(this);
      
      Automaton lCurrentAutomaton = mZeroAutomaton;
      
      for (int lTimes = 1; lTimes <= pUpperBound.getBound(); lTimes++) {
        lCurrentAutomaton = alternative(lCurrentAutomaton, repeatKTimes(lSubautomaton, lTimes));
      }
      
      return lCurrentAutomaton;
    }
    
    private Automaton repeatKTimes(Automaton pAutomaton, int k) {
      assert(pAutomaton != null);
      assert(k >= 0);
      
      switch (k) {
      case 0:
        return mZeroAutomaton;
      case 1:
        return pAutomaton;
      default:
        return concatenate(pAutomaton, repeatKTimes(pAutomaton, k - 1));
      }
    }
    
    private Automaton alternative(Automaton pLeftAutomaton, Automaton pRightAutomaton) {
      assert(pLeftAutomaton != null);
      assert(pRightAutomaton != null);
      
      Set<Integer> lInitialStates = new HashSet<Integer>(pLeftAutomaton.mInitialStates);
      
      for (int lInitialState : pRightAutomaton.mInitialStates) {
        lInitialStates.add(lInitialState + pLeftAutomaton.mNextFreeState);
      }
      
      Set<Integer> lFinalStates = new HashSet<Integer>(pLeftAutomaton.mFinalStates);
      
      for (int lFinalState : pRightAutomaton.mFinalStates) {
        lFinalStates.add(lFinalState + pLeftAutomaton.mNextFreeState);
      }
      
      DirectedGraph<Integer, AutomatonEdge> lTransitionRelation = new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class);
      
      for (Integer lState : pLeftAutomaton.mTransitionRelation.vertexSet()) {
        lTransitionRelation.addVertex(lState);
      }
      
      for (Integer lState : pRightAutomaton.mTransitionRelation.vertexSet()) {
        lTransitionRelation.addVertex(lState);
      }
      
      for (AutomatonEdge lEdge : pLeftAutomaton.mTransitionRelation.edgeSet()) {
        lEdge.duplicate(lEdge.getSource(), lEdge.getTarget(), lTransitionRelation);        
      }
      
      for (AutomatonEdge lEdge : pRightAutomaton.mTransitionRelation.edgeSet()) {
        lEdge.duplicate(lEdge.getSource() + pLeftAutomaton.mNextFreeState, lEdge.getTarget() + pLeftAutomaton.mNextFreeState, lTransitionRelation);
      }
      
      return new Automaton(lInitialStates, lFinalStates, lTransitionRelation, pLeftAutomaton.mNextFreeState + pRightAutomaton.mNextFreeState);
    }

    private Automaton kleeneStar(Automaton pAutomaton) {
      assert(pAutomaton != null);
      
      Set<Integer> lInitialStates = new HashSet<Integer>(pAutomaton.mInitialStates);
      Set<Integer> lFinalStates = new HashSet<Integer>(pAutomaton.mInitialStates);
      
      DirectedGraph<Integer, AutomatonEdge> lTransitionRelation = new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class);
      
      for (Integer lState : pAutomaton.mTransitionRelation.vertexSet()) {
        lTransitionRelation.addVertex(lState);
      }
      
      Set<AutomatonEdge> lEdgesToFinalStates = new HashSet<AutomatonEdge>();
      
      // get all edges leading to a final state in pAutomaton
      for (Integer lFinalState : pAutomaton.mFinalStates) {
        lEdgesToFinalStates.addAll(pAutomaton.mTransitionRelation.incomingEdgesOf(lFinalState));
      }
      
      // duplicate all edges that do not lead to a final state in pAutomaton
      for (AutomatonEdge lEdge : pAutomaton.mTransitionRelation.edgeSet()) {
        if (!pAutomaton.mFinalStates.contains(lEdge.getTarget())) {
          lEdge.duplicate(lEdge.getSource(), lEdge.getTarget(), lTransitionRelation);
        }
      }
      
      // redirect all edges leading to a final state in pAutomaton
      for (AutomatonEdge lEdge : lEdgesToFinalStates) {
        for (Integer lInitialState : pAutomaton.mInitialStates) {
          lEdge.duplicate(lEdge.getSource(), lInitialState, lTransitionRelation);
        }
      }
      
      return new Automaton(lInitialStates, lFinalStates, lTransitionRelation, pAutomaton.mNextFreeState);
    }
    
    private Automaton concatenate(Automaton pAutomaton1, Automaton pAutomaton2) {
      assert(pAutomaton1 != null);
      assert(pAutomaton2 != null);
      
      boolean lAreInitialAndFinalStatesDisjunct = true;
      
      for (Integer lFinalState : pAutomaton1.mFinalStates) {
        if (pAutomaton1.mInitialStates.contains(lFinalState)) {
          lAreInitialAndFinalStatesDisjunct = false;
          break;
        }
      }
      
      Set<Integer> lInitialStates = new HashSet<Integer>();
      
      lInitialStates.addAll(pAutomaton1.mInitialStates);
      
      if (!lAreInitialAndFinalStatesDisjunct) {
        lInitialStates.removeAll(pAutomaton1.mFinalStates);
        
        for (Integer lInitialState : pAutomaton2.mInitialStates) {
          lInitialStates.add(lInitialState + pAutomaton1.mNextFreeState);
        }
      }
      
      DirectedGraph<Integer, AutomatonEdge> lTransitionRelation = new DefaultDirectedGraph<Integer, AutomatonEdge>(AutomatonEdge.class);
      
      for (Integer lState : pAutomaton1.mTransitionRelation.vertexSet()) {
        if (!pAutomaton1.mFinalStates.contains(lState)) {
          lTransitionRelation.addVertex(lState);
        }
      }
      
      for (Integer lState : pAutomaton2.mTransitionRelation.vertexSet()) {
        lTransitionRelation.addVertex(lState + pAutomaton1.mNextFreeState);
      }
      
      Set<Integer> lFinalStates = new HashSet<Integer>();
      
      for (Integer lFinalState : pAutomaton2.mFinalStates) {
        lFinalStates.add(lFinalState + pAutomaton1.mNextFreeState);
      }
      
      // transitions from pAutomaton1
      for (AutomatonEdge lEdge : pAutomaton1.mTransitionRelation.edgeSet()) {
        Integer lSource = lEdge.getSource();
        Integer lTarget = lEdge.getTarget();
        
        if (!pAutomaton1.mFinalStates.contains(lSource) && !pAutomaton1.mFinalStates.contains(lTarget)) {
          lEdge.duplicate(lSource, lTarget, lTransitionRelation);
        }
        else if (pAutomaton1.mFinalStates.contains(lSource)) {
          if (pAutomaton1.mFinalStates.contains(lTarget)) {
            for (Integer lInitialState1 : pAutomaton2.mInitialStates) {
              for (Integer lInitialState2 : pAutomaton2.mInitialStates) {
                lEdge.duplicate(lInitialState1 + pAutomaton1.mNextFreeState, lInitialState2 + pAutomaton1.mNextFreeState, lTransitionRelation);
              }
            }
          }
          else {
            for (Integer lInitialState : pAutomaton2.mInitialStates) {
              lEdge.duplicate(lInitialState + pAutomaton1.mNextFreeState, lTarget, lTransitionRelation);
            }
          }
        }
        else {
          // pAutomaton1.mFinalStates.contains(lTarget) && !pAutomaton1.mFinalStates.contains(lSource)
          for (Integer lInitialState : pAutomaton2.mInitialStates) {
            lEdge.duplicate(lSource, lInitialState + pAutomaton1.mNextFreeState, lTransitionRelation);
          }
        }
      }
      
      // transitions from pAutomaton2
      for (AutomatonEdge lEdge : pAutomaton2.mTransitionRelation.edgeSet()) {
        lEdge.duplicate(lEdge.getSource() + pAutomaton1.mNextFreeState, lEdge.getTarget() + pAutomaton1.mNextFreeState, lTransitionRelation);
      }
      
      return new Automaton(lInitialStates, lFinalStates, lTransitionRelation, pAutomaton1.mNextFreeState + pAutomaton2.mNextFreeState);
    }
    
  }
}
