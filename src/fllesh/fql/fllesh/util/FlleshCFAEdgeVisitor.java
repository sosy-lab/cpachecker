package fllesh.fql.fllesh.util;

import fllesh.fql.fllesh.cpa.InternalSelfLoop;

public interface FlleshCFAEdgeVisitor<T> extends CFAEdgeVisitor<T> {

  public T visit(InternalSelfLoop pEdge);
  
}
