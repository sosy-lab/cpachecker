package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.InternalSelfLoop;

public interface FlleshCFAEdgeVisitor<T> extends CFAEdgeVisitor<T> {

  public T visit(InternalSelfLoop pEdge);
  
}
