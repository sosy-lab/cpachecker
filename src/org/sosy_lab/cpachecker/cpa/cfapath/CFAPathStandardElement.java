package org.sosy_lab.cpachecker.cpa.cfapath;

import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public class CFAPathStandardElement implements CFAPathElement, Iterable<CFAEdge> {

  private static final CFAPathStandardElement sEmptyPath = new CFAPathStandardElement();
  
  public static CFAPathStandardElement getEmptyPath() {
    return sEmptyPath;
  }
  
  private final CFAPathStandardElement mPredecessor;
  private final CFAEdge mCFAEdge;
  private final int mLength;
  
  private static class CFAEdgeIterator implements Iterator<CFAEdge> {

    private CFAPathStandardElement mCurrentElement;
    
    public CFAEdgeIterator(CFAPathStandardElement pLastElement) {
      mCurrentElement = pLastElement;
    }
    
    @Override
    public boolean hasNext() {
      return (mCurrentElement != sEmptyPath);
    }

    @Override
    public CFAEdge next() {
      CFAEdge lNextCFAEdge = mCurrentElement.mCFAEdge;
      
      mCurrentElement = mCurrentElement.mPredecessor;
      
      return lNextCFAEdge;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }
  
  private CFAPathStandardElement() {
    mPredecessor = null;
    mCFAEdge = null;
    mLength = 0;
  }
  
  public CFAPathStandardElement(CFAPathStandardElement pPredecessor, CFAEdge pCFAEdge) {
    if (pPredecessor == null) {
      throw new IllegalArgumentException();
    }
    
    if (pCFAEdge == null) {
      throw new IllegalArgumentException();
    }
    
    mPredecessor = pPredecessor;
    mCFAEdge = pCFAEdge;
    mLength = pPredecessor.getLength() + 1;
  }
  
  public int getLength() {
    return mLength;
  }
  
  public CFAEdge get(int lIndex) {
    if (lIndex >= mLength || lIndex < 0) {
      throw new IllegalArgumentException();
    }
    
    if (lIndex + 1 == mLength) {
      return mCFAEdge;
    }
    else {
      return mPredecessor.get(lIndex);
    }
  }

  @Override
  /*
   * Traverses the cfa path backwards.
   */
  public Iterator<CFAEdge> iterator() {
    return new CFAEdgeIterator(this);
  }
  
  public CFAEdge[] toArray() {
    CFAEdge[] lPath = new CFAEdge[mLength];
    
    CFAPathStandardElement lElement = this;
    
    for (int lIndex = mLength - 1; lIndex >= 0; lIndex--) {
      lPath[lIndex] = lElement.mCFAEdge;
      lElement = lElement.mPredecessor;
    }
    
    return lPath;
  }
  
  @Override
  public String toString() {
    if (getLength() == 0) {
      return "<>";
    }
    else {
      if (getLength() == 1) {
        return "< " + mCFAEdge.toString() + " >";
      }
      else {
        return "< ... " + mCFAEdge.toString() + " >";
      }
    }
  }

}
