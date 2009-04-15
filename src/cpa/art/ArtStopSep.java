package cpa.art;

import java.util.Collection;

import compositeCPA.CompositeStopOperator;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ArtStopSep implements StopOperator {

  private final ArtDomain domain;
  private final ConfigurableProgramAnalysis cpa;
  
  public ArtStopSep(AbstractDomain pArtDomain, ConfigurableProgramAnalysis cpa) {
    this.domain = (ArtDomain)pArtDomain;
    this.cpa = cpa;
  }
  
  @Override
  public <AE extends AbstractElement> boolean stop(AE pElement,
      Collection<AE> pReached, Precision pPrecision) throws CPAException {

    ArtElement artElement = (ArtElement)pElement;
    AbstractElement wrappedElement = artElement.getAbstractElementOnArtNode();
    StopOperator stopOp = cpa.getStopOperator();
    
    if(stopOp instanceof CompositeStopOperator){
      CompositeStopOperator compStopOp = (CompositeStopOperator) stopOp;
      if(compStopOp.containsBottomElement(wrappedElement)){
        return true;
      }
    }
    
    for (AbstractElement e : pReached) {
      if (stop(pElement, e)) {
        return true;
      }
    }
    return false;
  
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    ArtElement artElement = (ArtElement)pElement;
    AbstractElement wrappedElement = artElement.getAbstractElementOnArtNode();
    ArtElement reachedArtElement = (ArtElement)pReachedElement;
    AbstractElement wrappedReachedElement = reachedArtElement.getAbstractElementOnArtNode();
    StopOperator stopOp = cpa.getStopOperator();
    return stopOp.stop(wrappedElement, wrappedReachedElement);
  }
}
