package fql.fllesh.util;

import fql.fllesh.cpa.InternalSelfLoop;

public interface FlleshCFAEdgeVisitor<T> extends CFAEdgeVisitor<T> {

  public T visit(InternalSelfLoop pEdge);
  
}
