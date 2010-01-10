package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class RegularExpression implements Filter {

  String mRegularExpression;
  
  public RegularExpression(String pRegularExpression) {
    assert(pRegularExpression != null);
    
    mRegularExpression = pRegularExpression;
  }
  
  public String getRegularExpression() {
    return mRegularExpression;
  }
  
  @Override
  public String toString() {
    return "@REGEXP(" + mRegularExpression + ")";
  }
  
  @Override
  public int hashCode() {
    return 232134 + mRegularExpression.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof RegularExpression) {
      RegularExpression mRegularExpressionFilter = (RegularExpression)pOther;
      
      return mRegularExpression.equals(mRegularExpressionFilter.mRegularExpression);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
