package fllesh.fql.fllesh.util;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import fllesh.fql.fllesh.cpa.InternalSelfLoop;

public abstract class AbstractFlleshCFAEdgeVisitor<T> extends AbstractCFAEdgeVisitor<T> implements FlleshCFAEdgeVisitor<T> {

  @Override
  public T visit(CFAEdge pEdge) {
    if (pEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)) {
      if (pEdge instanceof InternalSelfLoop) {
        return visit((InternalSelfLoop)pEdge);
      }
      
      return visit((BlankEdge)pEdge);
    }
    
    return super.visit(pEdge);
  }
  
}
