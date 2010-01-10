package fql.frontend.ast.predicate;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.FQLNode;

public class Predicate implements FQLNode {
  public static enum Comparison {
    GREATER_OR_EQUAL,
    GREATER,
    EQUAL,
    LESS_OR_EQUAL,
    LESS,
    NOT_EQUAL
  }
  
  private Comparison mComparison;
  private Term mLeftTerm;
  private Term mRightTerm;
  private String mString;
  
  public Predicate(Term pLeftTerm, Comparison pComparison, Term pRightTerm) {
    assert(pLeftTerm != null);
    assert(pComparison != null);
    assert(pRightTerm != null);
    
    mLeftTerm = pLeftTerm;
    mComparison = pComparison;
    mRightTerm = pRightTerm;
    
    String lComparisonString = null;
    
    switch (mComparison) {
    case GREATER_OR_EQUAL:
      lComparisonString = ">=";
      break;
    case GREATER:
      lComparisonString = ">";
      break;
    case EQUAL:
      lComparisonString = "==";
      break;
    case LESS_OR_EQUAL:
      lComparisonString = "<=";
      break;
    case LESS:
      lComparisonString = "<";
      break;
    case NOT_EQUAL:
      lComparisonString = "!=";
      break;
    }
    
    mString = mLeftTerm.toString() + " " + lComparisonString + " " + mRightTerm.toString();
  }
  
  public Comparison getComparison() {
    return mComparison;
  }
  
  public Term getLeftTerm() {
    return mLeftTerm;
  }
  
  public Term getRightTerm() {
    return mRightTerm;
  }
  
  @Override
  public String toString() {
    return mString;
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
      Predicate lOther = (Predicate)pOther;
      
      return (mLeftTerm.equals(lOther.mLeftTerm) && mRightTerm.equals(lOther.mRightTerm) && mComparison.equals(lOther.mComparison));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 3045820 + mLeftTerm.hashCode() + mComparison.hashCode() + mRightTerm.hashCode();
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }
  
}
