package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class CompoundBottomElement extends CompoundElement {

  public static CompoundBottomElement create(List<AbstractDomain> pDomains) {
    List<AbstractElement> lBottomElements = new ArrayList<AbstractElement>(pDomains.size());
    
    for (AbstractDomain lSubdomain : pDomains) {
      lBottomElements.add(lSubdomain.getBottomElement());
    }
    
    return new CompoundBottomElement(lBottomElements);
  }
  
  private CompoundBottomElement(List<AbstractElement> pElements) {
    super(pElements);
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!getClass().equals(pOther.getClass())) {
      return false;
    }
    
    CompoundBottomElement lOther = (CompoundBottomElement)pOther;
    
    if (size() != lOther.size()) {
      return false;
    }
    
    for (int lIndex = 0; lIndex < size(); lIndex++) {
      if (!lOther.getSubelement(lIndex).equals(getSubelement(lIndex))) {
        return false;
      }
    }
    
    return true;
  }

}
