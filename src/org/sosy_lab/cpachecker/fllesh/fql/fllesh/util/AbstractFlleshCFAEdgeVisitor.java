package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.InternalSelfLoop;

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
