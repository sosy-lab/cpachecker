package org.sosy_lab.cpachecker.fllesh.ecp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class ECPNodeSet implements ECPGuard, Iterable<CFANode> {
  
  private Set<CFANode> mCFANodes;
  
  public ECPNodeSet(Set<CFANode> pCFANodes) {
    mCFANodes = new HashSet<CFANode>();
    mCFANodes.addAll(pCFANodes);
  }
  
  @Override
  public int hashCode() {
    return mCFANodes.hashCode();
  }
  
  @Override 
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther instanceof ECPNodeSet) {
      ECPNodeSet lECPNodeSet = (ECPNodeSet)pOther;
      
      return mCFANodes.equals(lECPNodeSet.mCFANodes); 
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return mCFANodes.toString();
  }

  @Override
  public Iterator<CFANode> iterator() {
    return mCFANodes.iterator();
  }
  
}
