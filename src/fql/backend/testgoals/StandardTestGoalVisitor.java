package fql.backend.testgoals;

import fql.backend.targetgraph.Edge;
import fql.backend.targetgraph.Node;

public class StandardTestGoalVisitor<T> implements TestGoalVisitor<T> {

  @Override
  public T visit(Node pNode) {
    throw new UnsupportedOperationException("visit(Node pNode) not implemented!");
  }

  @Override
  public T visit(Edge pEdge) {
    throw new UnsupportedOperationException("visit(Edge pEdge) not implemented!");
  }

  @Override
  public T visit(EdgeSequence pEdgeSequence) {
    throw new UnsupportedOperationException("visit(EdgeSequence pEdgeSequence) not implemented");
  }

}
