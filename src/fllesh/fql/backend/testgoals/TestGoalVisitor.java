package fql.backend.testgoals;

import fql.backend.targetgraph.Edge;
import fql.backend.targetgraph.Node;

public interface TestGoalVisitor<T> {

  public T visit(Node pNode);
  public T visit(Edge pEdge);
  public T visit(EdgeSequence pEdgeSequence);
  
}
