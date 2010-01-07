package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class EnclosingScopes implements Filter {

  private Filter mFilter;
  
  public EnclosingScopes(Filter pFilter) {
    assert(pFilter != null);
    
    mFilter = pFilter;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  @Override
  public String toString() {
    return "ENCLOSING_SCOPES(" + mFilter.toString() + ")";
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    
    EnclosingScopes other = (EnclosingScopes) obj;
    
    return mFilter.equals(other.mFilter);
  }

  @Override
  public int hashCode() {
    return 476455 + mFilter.hashCode();
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }

}
