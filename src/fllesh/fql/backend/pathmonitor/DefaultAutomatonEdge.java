package fllesh.fql.backend.pathmonitor;

import org.jgrapht.DirectedGraph;

public abstract class DefaultAutomatonEdge implements AutomatonEdge {
  
  private Integer mSource;
  private Integer mTarget;
  
  public DefaultAutomatonEdge(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation) {
    assert(pSource != null);
    assert(pTarget != null);
    assert(pTransitionRelation != null);
    
    mSource = pSource;
    mTarget = pTarget;
    
    pTransitionRelation.addVertex(mSource);
    pTransitionRelation.addVertex(mTarget);
    pTransitionRelation.addEdge(mSource, mTarget, this);
  }
  
  public Integer getSource() {
    return mSource;
  }
  
  public Integer getTarget() {
    return mTarget;
  }
  
}
