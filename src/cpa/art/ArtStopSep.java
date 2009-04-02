package cpa.art;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ArtStopSep implements StopOperator {

  private final ArtDomain domain;
  
  public ArtStopSep(AbstractDomain pArtDomain) {
    this.domain = (ArtDomain)pArtDomain;
  }
  
  @Override
  public <AE extends AbstractElement> boolean stop(AE pElement,
      Collection<AE> pReached, Precision pPrecision) throws CPAException {
    for (AbstractElement testElement : pReached) {
      if (stop(pElement, testElement)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    PartialOrder partialOrder = domain.getPartialOrder ();
    if (partialOrder.satisfiesPartialOrder (pElement, pReachedElement))
      return true;
    return false;
  }

}
