package fllesh.fql.backend.pathmonitor;

import org.jgrapht.DirectedGraph;

import fllesh.fql.frontend.ast.predicate.Predicates;

public class PredicatesEdge extends DefaultAutomatonEdge {

  private Predicates mPredicates;
  
  public PredicatesEdge(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation, Predicates pPredicates) {
    super(pSource, pTarget, pTransitionRelation);
    
    assert(pPredicates != null);
    //assert(!pPredicates.isEmpty());
    
    mPredicates = pPredicates;
  }
  
  public Predicates getPredicate() {
    return mPredicates;
  }
  
  @Override
  public String toString() {
    return getSource().toString() + "-<Predicates>->" + getTarget().toString();
  }
  
  @Override
  public <T> T accept(AutomatonEdgeVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

  @Override
  public AutomatonEdge duplicate(Integer pSource, Integer pTarget,
      DirectedGraph<Integer, AutomatonEdge> pTransitionRelation) {
    return new PredicatesEdge(pSource, pTarget, pTransitionRelation, mPredicates);
  }

}
