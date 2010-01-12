package cpa.mustmay;

import cpa.common.interfaces.Precision;

public class MustMayAnalysisPrecision implements Precision {
  Precision mMustPrecision;
  Precision mMayPrecision;
  
  public MustMayAnalysisPrecision(Precision pMustPrecision, Precision pMayPrecision) {
    assert(pMustPrecision != null);
    assert(pMayPrecision != null);
    
    mMustPrecision = pMustPrecision;
    mMayPrecision = pMayPrecision;
  }
  
  public Precision getMustPrecision() {
    return mMustPrecision;
  }
  
  public Precision getMayPrecision() {
    return mMayPrecision;
  }
  
  @Override
  public String toString() {
    return "<" + mMustPrecision.toString() + ", " + mMayPrecision.toString() + ">";
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
      MustMayAnalysisPrecision lPrecision = (MustMayAnalysisPrecision)pOther;
      
      Precision lMustPrecision = lPrecision.getMustPrecision();
      Precision lMayPrecision = lPrecision.getMayPrecision();
      
      return (mMustPrecision.equals(lMustPrecision) && mMayPrecision.equals(lMayPrecision));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mMustPrecision.hashCode() + mMayPrecision.hashCode();
  }
  
}
