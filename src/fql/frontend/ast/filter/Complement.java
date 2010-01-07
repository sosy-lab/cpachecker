package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Complement implements Filter {

  private Filter mFilter;
  
  public Complement(Filter pFilter) {
    assert(pFilter != null);
    
    mFilter = pFilter;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  @Override
  public String toString() {
    return "COMPLEMENT(" + mFilter.toString() + ")";
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    
    Complement other = (Complement) obj;
    
    return mFilter.equals(other.mFilter);
  }

  @Override
  public int hashCode() {
    return 155 + mFilter.hashCode();
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }

}
