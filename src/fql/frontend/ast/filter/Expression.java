package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Expression implements Filter {

  String mExpression;
  
  public Expression(String pExpression) {
    assert(pExpression != null);
    
    mExpression = pExpression;
  }
  
  public String getExpression() {
    return mExpression;
  }
  
  @Override
  public String toString() {
    return "@EXPR(" + mExpression + ")";
  }
  
  @Override
  public int hashCode() {
    return 78674 + mExpression.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof Expression) {
      Expression mExpressionFilter = (Expression)pOther;
      
      return mExpression.equals(mExpressionFilter.mExpression);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
