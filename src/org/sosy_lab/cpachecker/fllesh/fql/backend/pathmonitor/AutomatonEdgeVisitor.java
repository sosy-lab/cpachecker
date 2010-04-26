package org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor;

public interface AutomatonEdgeVisitor<T> {
  
  public T visit(TargetGraphEdge pEdge);
  
  public T visit(PredicatesEdge pEdge);
  
}
