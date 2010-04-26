package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.FQLNode;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Coverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.PathMonitor;

public class Query implements FQLNode {

  private Coverage mCoverage;
  private PathMonitor mMonitor;
  
  public Query(Coverage pCoverage, PathMonitor pPassingMonitor) {
    assert(pCoverage != null);
    assert(pPassingMonitor != null);
    
    mCoverage = pCoverage;
    mMonitor = pPassingMonitor;
  }
  
  public Coverage getCoverage() {
    return mCoverage;
  }
  
  public PathMonitor getPassingMonitor() {
    return mMonitor;
  }
  
  @Override
  public String toString() {
    return "COVER " + mCoverage.toString() + " PASSING " + mMonitor.toString();
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
      Query lQuery = (Query)pOther;
      
      return mCoverage.equals(lQuery.mCoverage) && mMonitor.equals(lQuery.mMonitor);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 32423 + mCoverage.hashCode() + mMonitor.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
