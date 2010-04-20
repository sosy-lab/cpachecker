package fllesh.fql.frontend.ast.coverage;

import fllesh.fql.frontend.ast.ASTVisitor;

public class Intersection implements Coverage {

  private Coverage mCoverage1;
  private Coverage mCoverage2;
  
  public Intersection(Coverage pLeftCoverage, Coverage pRightCoverage) {
    assert(pLeftCoverage != null);
    assert(pRightCoverage != null);
    
    mCoverage1 = pLeftCoverage;
    mCoverage2 = pRightCoverage;
  }
  
  public Coverage getLeftCoverage() {
    return mCoverage1;
  }
  
  public Coverage getRightCoverage() {
    return mCoverage2;
  }
  
  @Override
  public String toString() {
    return "INTERSECT(" + mCoverage1.toString() + ", " + mCoverage2.toString() + ")";
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
      Intersection lOther = (Intersection)pOther;
      
      return lOther.mCoverage1.equals(mCoverage1) && lOther.mCoverage2.equals(mCoverage2);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 92877 + mCoverage1.hashCode() + mCoverage2.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
