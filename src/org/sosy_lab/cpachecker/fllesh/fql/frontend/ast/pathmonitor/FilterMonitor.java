package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;

public class FilterMonitor implements PathMonitor {

  private Filter mFilter;
  
  public FilterMonitor(Filter pFilter) {
    mFilter = pFilter;
  }
  
  public Filter getFilter() {
    return mFilter;
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
    
    FilterMonitor lMonitor = (FilterMonitor)pOther;
    
    return mFilter.equals(lMonitor.mFilter);
  }
  
  @Override
  public int hashCode() {
    return mFilter.hashCode() + 234234;
  }
  
  @Override
  public String toString() {
    return mFilter.toString();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
