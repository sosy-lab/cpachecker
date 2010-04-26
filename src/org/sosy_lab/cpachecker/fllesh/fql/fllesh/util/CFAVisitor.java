package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public interface CFAVisitor {
  
  public void init(CFANode pInitialNode);
  public void visit(CFAEdge p);
  
}
