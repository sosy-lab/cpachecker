package fllesh.fql.backend.pathmonitor;

import java.util.Set;

import org.jgrapht.DirectedGraph;

import fllesh.fql.backend.targetgraph.Edge;
import fllesh.fql.backend.targetgraph.TargetGraph;

public class TargetGraphEdge extends DefaultAutomatonEdge {

  private TargetGraph mTargetGraph;
  
  public TargetGraphEdge(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation, TargetGraph pTargetGraph) {
    super(pSource, pTarget, pTransitionRelation);
    
    assert(pTargetGraph != null);
    
    mTargetGraph = pTargetGraph;
  }
  
  public TargetGraphEdge duplicate(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation) {
    return new TargetGraphEdge(pSource, pTarget, pTransitionRelation, mTargetGraph);
  }
  
  // TODO: we should find more efficient ways to query for target graph edges
  // maybe storing the edges with respect to the associated cfa edge.
  public Set<Edge> getEdges() {
    return mTargetGraph.getEdges();
  }
  
  @Override
  public String toString() {
    return getSource().toString() + "-<TargetGraph>->" + getTarget().toString();
  }
  
  @Override
  public <T> T accept(AutomatonEdgeVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
