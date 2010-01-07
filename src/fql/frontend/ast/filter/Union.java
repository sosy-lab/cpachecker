package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Union implements Filter {

  private Filter mFilter1;
  private Filter mFilter2;
  
  public Union(Filter pFilter1, Filter pFilter2) {
    assert(pFilter1 != null);
    assert(pFilter2 != null);
    
    mFilter1 = pFilter1;
    mFilter2 = pFilter2;
  }
  
  public Filter getFirstFilter() {
    return mFilter1;
  }
  
  public Filter getSecondFilter() {
    return mFilter2;
  }
  
  @Override
  public String toString() {
    return "UNION(" + mFilter1.toString() + ", " + mFilter2.toString() + ")";
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
    Union other = (Union) obj;
    
    return (mFilter1.equals(other.mFilter1) && mFilter2.equals(other.mFilter2));
  }

  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }

}
