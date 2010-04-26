package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;

public class Alternative implements PathMonitor {

  private PathMonitor mSubmonitor1;
  private PathMonitor mSubmonitor2;
  
  public Alternative(PathMonitor pSubmonitor1, PathMonitor pSubmonitor2) {
    assert(pSubmonitor1 != null);
    assert(pSubmonitor2 != null);
    
    mSubmonitor1 = pSubmonitor1;
    mSubmonitor2 = pSubmonitor2;
  }
  
  public PathMonitor getLeftSubmonitor() {
    return mSubmonitor1;
  }
  
  public PathMonitor getRightSubmonitor() {
    return mSubmonitor2;
  }
  
  @Override
  public String toString() {
    return mSubmonitor1.toString() + "+" + mSubmonitor2.toString();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Alternative lMonitor = (Alternative)pOther;
      
      return lMonitor.mSubmonitor1.equals(mSubmonitor1)
              && lMonitor.mSubmonitor2.equals(mSubmonitor2);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 9328423 + mSubmonitor1.hashCode() + mSubmonitor2.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
