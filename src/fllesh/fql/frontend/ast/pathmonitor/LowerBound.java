package fllesh.fql.frontend.ast.pathmonitor;

import fllesh.fql.frontend.ast.ASTVisitor;

public class LowerBound implements PathMonitor {

  private PathMonitor mSubmonitor;
  private int mBound;
  
  public LowerBound(PathMonitor pSubmonitor, int pBound) {
    assert(pSubmonitor != null);
    assert(pBound >= 0);
    
    mSubmonitor = pSubmonitor;
    mBound = pBound;
  }
  
  public int getBound() {
    return mBound;
  }
  
  public PathMonitor getSubmonitor() {
    return mSubmonitor;
  }
  
  @Override
  public String toString() {
    return "(" + mSubmonitor.toString() + ")>=" + mBound;
  }
  
  @Override
  public int hashCode() {
    return 39202 + mSubmonitor.hashCode() + mBound;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther == this) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      LowerBound lUpperBound = (LowerBound)pOther;
      
      return lUpperBound.mSubmonitor.equals(mSubmonitor) && (lUpperBound.mBound == mBound);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
