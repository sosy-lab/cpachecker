package fllesh.fql.backend.testgoals;

import fllesh.fql.backend.targetgraph.Edge;
import fllesh.fql.backend.targetgraph.Node;

public interface TestGoalVisitor<T> {

  public T visit(Node pNode);
  public T visit(Edge pEdge);
  public T visit(EdgeSequence pEdgeSequence);
  
}
