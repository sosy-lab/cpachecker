package org.sosy_lab.cpachecker.fllesh.fql2.ast.filter;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;

public class Predication implements Filter {

  private Filter mFilter;
  private Predicate mPredicate;
  
  public Predication(Filter pFilter, Predicate pPredicate) {
    mFilter = pFilter;
    mPredicate = pPredicate;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  public Predicate getPredicate() {
    return mPredicate;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() != getClass()) {
      return false;
    }
    
    Predication lOther = (Predication)pOther;
    
    return mFilter.equals(lOther.mFilter) && mPredicate.equals(lOther.mPredicate);
  }
  
  @Override
  public int hashCode() {
    return mFilter.hashCode() + mPredicate.hashCode() + 243;
  }
  
  @Override
  public String toString() {
    return "PRED(" + mFilter.toString() + ", " + mPredicate.toString() + ")";
  }
  
  @Override
  public <T> T accept(FilterVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
