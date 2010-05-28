package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompoundStopOperator implements StopOperator {

  private List<StopOperator> mStopOperators;
  
  public CompoundStopOperator(List<StopOperator> pStopOperators) {
    mStopOperators = new ArrayList<StopOperator>(pStopOperators);
  }
  
  @Override
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision)
      throws CPAException {
    
    for (AbstractElement lReachedElement : pReached) {
      if (stop(pElement, lReachedElement)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {

    CompoundElement lElement = (CompoundElement)pElement;
    CompoundElement lReachedElement = (CompoundElement)pReachedElement;
    
    for (int lIndex = 0; lIndex < lElement.size(); lIndex++) {
      if (!mStopOperators.get(lIndex).stop(lElement.getSubelement(lIndex), lReachedElement.getSubelement(lIndex))) {
        return false;
      }
    }
    
    return true;
  }

}
