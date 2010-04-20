package fql.fllesh.util;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import fql.fllesh.cpa.InternalSelfLoop;

public abstract class AbstractCFAEdgeVisitor<T> implements CFAEdgeVisitor<T> {

  @Override
  public T visit(CFAEdge pEdge) {
    switch (pEdge.getEdgeType()) {
    case AssumeEdge:
      return visit((AssumeEdge)pEdge);
    case BlankEdge:
      if (pEdge instanceof InternalSelfLoop) {
        return visit((InternalSelfLoop)pEdge);
      }
      
      return visit((BlankEdge)pEdge);
    case CallToReturnEdge:
      return visit((CallToReturnEdge)pEdge);
    case DeclarationEdge:
      return visit((DeclarationEdge)pEdge);
    case FunctionCallEdge:
      return visit((FunctionCallEdge)pEdge);
    case MultiDeclarationEdge:
      return visit((MultiDeclarationEdge)pEdge);
    case MultiStatementEdge:
      return visit((MultiStatementEdge)pEdge);
    case ReturnEdge:
      return visit((ReturnEdge)pEdge);
    case StatementEdge:
      return visit((StatementEdge)pEdge);         
    }
    
    // this should not happen
    throw new IllegalArgumentException(pEdge.toString() + " not supported!");
  }
  
}
