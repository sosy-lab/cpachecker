package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;

public class Compose implements Filter {

  private Filter mFilter1;
  private Filter mFilter2;
  
  public Compose(Filter pFilter1, Filter pFilter2) {
    assert(pFilter1 != null);
    assert(pFilter2 != null);
    
    mFilter1 = pFilter1;
    mFilter2 = pFilter2;
  }
  
  public Filter getFilter1() {
    return mFilter1;
  }
  
  public Filter getFilter2() {
    return mFilter2;
  }
  
  public Filter getFilterAppliedSecond() {
    return mFilter1;
  }
  
  public Filter getFilterAppliedFirst() {
    return mFilter2;
  }
  
  @Override
  public String toString() {
    return "COMPOSE(" + mFilter1.toString() + ", " + mFilter2.toString() + ")";
  }
  
  @Override
  public int hashCode() {
    return 527 + mFilter1.hashCode() + mFilter2.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Compose other = (Compose) obj;
    
    return (mFilter1.equals(other.mFilter1) && mFilter2.equals(other.mFilter2));
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
