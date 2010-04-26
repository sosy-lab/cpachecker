package org.sosy_lab.cpachecker.cpa.mustmay;

import java.util.ArrayList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;

public class MustMayAnalysisElement implements AbstractElement, AbstractWrapperElement {

  AbstractElement mMustElement;
  AbstractElement mMayElement;
  
  ArrayList<AbstractElement> mWrappedElements;
  
  public MustMayAnalysisElement(AbstractElement pMustElement, AbstractElement pMayElement) {
    assert(pMustElement != null);
    assert(pMayElement != null);
    
    mMustElement = pMustElement;
    mMayElement = pMayElement;
    
    mWrappedElements = new ArrayList<AbstractElement>();
    mWrappedElements.add(mMustElement);
    mWrappedElements.add(mMayElement);
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

  @Override
  public Iterable<? extends AbstractElement> getWrappedElements() {
    return mWrappedElements;
  }

  @Override
  public <T extends AbstractElement> T retrieveWrappedElement(Class<T> pType) {

    // TODO: should retrieveWrappedElement return itself if this is a subtype of pType?
    
    for (AbstractElement lElement : mWrappedElements) {
      if (pType.isAssignableFrom(lElement.getClass())) {
        return pType.cast(lElement);
      } 
      else if (lElement instanceof AbstractWrapperElement) {
        T lResult = ((AbstractWrapperElement)lElement).retrieveWrappedElement(pType);
        
        if (lResult != null) {
          return lResult;
        }
      }  
    }
    
    return null;
  }

  @Override
  public AbstractElementWithLocation retrieveLocationElement() {
    // TODO: think about what to do here
    assert(false);
    
    // TODO Auto-generated method stub
    return null;
  }

}
