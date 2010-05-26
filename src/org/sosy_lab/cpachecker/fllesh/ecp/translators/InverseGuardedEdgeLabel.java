package org.sosy_lab.cpachecker.fllesh.ecp.translators;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public class InverseGuardedEdgeLabel extends GuardedEdgeLabel {

  public InverseGuardedEdgeLabel(GuardedEdgeLabel pGuard) {
    super(pGuard);    
  }

  @Override
  public boolean contains(CFAEdge pCFAEdge) {
    return !super.contains(pCFAEdge);
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!pOther.getClass().equals(getClass())) {
      return false;
    }
    
    InverseGuardedEdgeLabel lOther = (InverseGuardedEdgeLabel)pOther;
    
    return getGuards().equals(lOther.getGuards()) && getEdgeSet().equals(lOther.getEdgeSet());
  }
  
  @Override
  public int hashCode() {
    return super.hashCode() + 232;
  }
  
  @Override
  public String toString() {
    return "!" + super.toString();
  }
  
}
