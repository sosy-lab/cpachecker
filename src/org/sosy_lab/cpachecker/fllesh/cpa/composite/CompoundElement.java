package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

public class CompoundElement implements AbstractWrapperElement, Targetable {
  
  private List<AbstractElement> mElements;
  
  /*
   * TODO introduce special bottom element and do not allow  
   * single bottom elements in compound elements
   */
  public CompoundElement(List<AbstractElement> pElements) {
    mElements = new ArrayList<AbstractElement>(pElements);
  }
  
  public CompoundElement(AbstractElement... pElements) {
    mElements = new ArrayList<AbstractElement>(pElements.length);
    
    for (int lIndex = 0; lIndex < pElements.length; lIndex++) {
      mElements.add(pElements[lIndex]);
    }
  }
  
  public AbstractElement getSubelement(int lIndex) {
    return mElements.get(lIndex);
  }
  
  public int size() {
    return mElements.size();
  }
  
  @Override
  public Iterable<? extends AbstractElement> getWrappedElements() {
    return mElements;
  }

  @Override
  public AbstractElementWithLocation retrieveLocationElement() {
    return retrieveWrappedElement(AbstractElementWithLocation.class);
  }

  @Override
  public <T extends AbstractElement> T retrieveWrappedElement(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    
    for (AbstractElement lElement : mElements) {
      if (pType.isAssignableFrom(lElement.getClass())) {
        return pType.cast(lElement);
      } else if (lElement instanceof AbstractWrapperElement) {
        T lResult = ((AbstractWrapperElement)lElement).retrieveWrappedElement(pType);
        if (lResult != null) {
          return lResult;
        }
      }
    }
    
    return null;
  }

  @Override
  public boolean isTarget() {
    for (AbstractElement lSubelement : mElements) {
      if ((lSubelement instanceof Targetable) && ((Targetable)lSubelement).isTarget()) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!pOther.getClass().equals(getClass())) {
      return false;
    }
   
    CompoundElement lOther = (CompoundElement)pOther;
    
    return mElements.equals(lOther.mElements);
  }
  
  @Override
  public int hashCode() {
    return mElements.hashCode() + 230328;
  }
  
  @Override
  public String toString() {
    StringBuffer lResult = new StringBuffer();
    
    lResult.append("(");
    
    boolean lIsFirst = true;
    
    for (AbstractElement lSubelement : mElements) {
      if (lIsFirst) {
        lIsFirst = false;
      }
      else {
        lResult.append(", ");
      }
      
      lResult.append(lSubelement.toString());
    }
    
    lResult.append(")");
    
    return lResult.toString();
  }
  
}
