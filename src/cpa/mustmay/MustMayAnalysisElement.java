package cpa.mustmay;

import cpa.common.interfaces.AbstractElement;

public class MustMayAnalysisElement implements AbstractElement {

  AbstractElement mMustElement;
  AbstractElement mMayElement;
  
  public MustMayAnalysisElement(AbstractElement pMustElement, AbstractElement pMayElement) {
    assert(pMustElement != null);
    assert(pMayElement != null);
    
    mMustElement = pMustElement;
    mMayElement = pMayElement;
  }
  
  public AbstractElement getMustElement() {
    return mMustElement;
  }
  
  public AbstractElement getMayElement() {
    return mMayElement;
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
      MustMayAnalysisElement lElement = (MustMayAnalysisElement)pOther;
      
      AbstractElement lAbstractMustElement = lElement.mMustElement;
      AbstractElement lAbstractMayElement = lElement.mMayElement;
      
      return lAbstractMustElement.equals(mMustElement) && lAbstractMayElement.equals(mMayElement);
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    return mMustElement.hashCode() + mMayElement.hashCode();
  }
  
  @Override
  public String toString() {
    return "[must: " + mMustElement.toString() + ", may: " + mMayElement.toString() + "]";
  }
  
  @Override
  public boolean isError() {
    return false;
  }

}
