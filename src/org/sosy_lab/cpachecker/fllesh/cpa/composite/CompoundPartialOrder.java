package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompoundPartialOrder {

  private List<AbstractDomain> mDomains;
  
  public CompoundPartialOrder(List<AbstractDomain> pDomains) {
    mDomains = new ArrayList<AbstractDomain>(pDomains);
  }
  
  public boolean satisfiesPartialOrder(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    CompoundElement lElement1 = (CompoundElement)pElement1;
    CompoundElement lElement2 = (CompoundElement)pElement2;
    
    for (int lIndex = 0; lIndex < mDomains.size(); lIndex++) {
      if (!mDomains.get(lIndex).satisfiesPartialOrder(lElement1.getSubelement(lIndex), lElement2.getSubelement(lIndex))) {
        return false;
      }
    }
    
    return true;
  }

}
