package org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals;

import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;

public interface TestGoalVisitor<T> {

  public T visit(Node pNode);
  public T visit(Edge pEdge);
  public T visit(EdgeSequence pEdgeSequence);
  
}
