package fllesh.fql.fllesh.util;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

public interface CFAVisitor {
  
  public void init(CFANode pInitialNode);
  public void visit(CFAEdge p);
  
}
