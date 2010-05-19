package org.sosy_lab.cpachecker.fllesh.ecp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public class ECPEdgeSet implements ECPAtom, Iterable<CFAEdge> {

  private Set<CFAEdge> mCFAEdges;
  
  public ECPEdgeSet(Collection<CFAEdge> pCFAEdge) {
    mCFAEdges = new HashSet<CFAEdge>();
    mCFAEdges.addAll(pCFAEdge);
  }
  
  @Override
  public int hashCode() {
    return mCFAEdges.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther instanceof ECPEdgeSet) {
      ECPEdgeSet lOther = (ECPEdgeSet)pOther;
      
      return mCFAEdges.equals(lOther.mCFAEdges);
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return mCFAEdges.toString();
  }
  
  @Override
  public Iterator<CFAEdge> iterator() {
    return mCFAEdges.iterator();
  }

}
