package cpa.art;

import java.util.Collection;

import compositeCPA.CompositeStopOperator;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ARTStopSep implements StopOperator {

  private final ARTDomain domain;
  private final ConfigurableProgramAnalysis wrappedCpa;

  public ARTStopSep(AbstractDomain pArtDomain, ConfigurableProgramAnalysis cpa) {
    this.domain = (ARTDomain)pArtDomain;
    this.wrappedCpa = cpa;
  }

  @Override
  public <AE extends AbstractElement> boolean stop(AE pElement,
      Collection<AE> pReached, Precision pPrecision) throws CPAException {

    ARTElement artElement = (ARTElement)pElement;
    AbstractElement wrappedElement = artElement.getAbstractElementOnArtNode();
    StopOperator stopOp = wrappedCpa.getStopOperator();

    if(stopOp instanceof CompositeStopOperator){
      CompositeStopOperator compStopOp = (CompositeStopOperator) stopOp;
      if(compStopOp.containsBottomElement(wrappedElement)){
        return true;
      }
    }

    for (AbstractElement e : pReached) {
      if (stop(pElement, e)) {
        artElement.setCovered(true);
        ((ARTCPA)domain.getCpa()).setCovered(artElement);
        return true;
      }
    }
    return false;

  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
  throws CPAException {
    ARTElement artElement = (ARTElement)pElement;
    AbstractElement wrappedElement = artElement.getAbstractElementOnArtNode();
    ARTElement reachedArtElement = (ARTElement)pReachedElement;

    if (!reachedArtElement.isMarked()) {
      return false;
    }

    AbstractElement wrappedReachedElement = reachedArtElement.getAbstractElementOnArtNode();
    StopOperator stopOp = wrappedCpa.getStopOperator();
    return stopOp.stop(wrappedElement, wrappedReachedElement);
  }
}
