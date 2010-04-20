package fllesh.fql.backend.pathmonitor;

public class DefaultAutomatonEdgeVisitor<T> implements AutomatonEdgeVisitor<T> {

  @Override
  public T visit(TargetGraphEdge pEdge) {
    throw new UnsupportedOperationException("visit(TargetGraphEdge pEdge) is not implemented!");
  }

  @Override
  public T visit(PredicatesEdge pEdge) {
    throw new UnsupportedOperationException("visit(PredicateEdge pEdge) is not implemented!");
  }

}
