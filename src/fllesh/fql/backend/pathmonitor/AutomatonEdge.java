package fllesh.fql.backend.pathmonitor;

import org.jgrapht.DirectedGraph;

public interface AutomatonEdge {
  
  public AutomatonEdge duplicate(Integer pSource, Integer pTarget, DirectedGraph<Integer, AutomatonEdge> pTransitionRelation);
  public <T> T accept(AutomatonEdgeVisitor<T> pVisitor);
  public Integer getSource();
  public Integer getTarget();
  
}
