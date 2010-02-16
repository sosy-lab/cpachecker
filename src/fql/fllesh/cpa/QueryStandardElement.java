package fql.fllesh.cpa;

import cpa.mustmay.MustMayAnalysisElement;

public class QueryStandardElement implements QueryElement {

  private Integer mAutomatonState1;
  private boolean mMustState1;
  
  private Integer mAutomatonState2;
  private boolean mMustState2;
  
  private MustMayAnalysisElement mDataSpace;
  
  public QueryStandardElement(Integer pAutomatonState1, boolean pMustState1, Integer pAutomatonState2, boolean pMustState2, MustMayAnalysisElement pDataSpace) {
    assert(pAutomatonState1 != null);
    assert(pAutomatonState2 != null);
    assert(pDataSpace != null);
    
    mAutomatonState1 = pAutomatonState1;
    mMustState1 = pMustState1;
    
    mAutomatonState2 = pAutomatonState2;
    mMustState2 = pMustState2;
    
    mDataSpace = pDataSpace;
  }
  
  public Integer getAutomatonState1() {
    return mAutomatonState1;
  }
  
  public Integer getAutomatonState2() {
    return mAutomatonState2;
  }
  
  public boolean getMustState1() {
    return mMustState1;
  }
  
  public boolean getMustState2() {
    return mMustState2;
  }
  
  public MustMayAnalysisElement getDataSpace() {
    return mDataSpace;
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
      QueryStandardElement lElement = (QueryStandardElement)pOther;
      
      return mAutomatonState1.equals(lElement.mAutomatonState1)
              && mMustState1 == lElement.mMustState1
              && mAutomatonState2.equals(lElement.mAutomatonState2)
              && mMustState2 == lElement.mMustState2
              && mDataSpace.equals(lElement.mDataSpace);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 239438 + mAutomatonState1.hashCode() + mAutomatonState2.hashCode() + mDataSpace.hashCode() + (mMustState1?1:0) + (mMustState2?1:0);
  }
  
  @Override
  public String toString() {
    return "( <" + (mMustState1?mAutomatonState1.toString():"bot") + ", " + mAutomatonState1.toString() + ">, <" + (mMustState2?mAutomatonState2.toString():"bot") + ", " + mAutomatonState2.toString() + ">, " + mDataSpace.toString() + ")";
  }
  
  @Override
  public boolean isError() {
    // TODO Auto-generated method stub
    return false;
  }

}
