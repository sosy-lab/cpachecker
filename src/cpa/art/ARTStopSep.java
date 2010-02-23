package cpa.art;

import java.util.Collection;

import compositeCPA.CompositeStopOperator;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ARTStopSep implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public ARTStopSep(ConfigurableProgramAnalysis cpa) {
    this.wrappedCpa = cpa;
  }

  @Override
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision) throws CPAException {

    ARTElement artElement = (ARTElement)pElement;
    AbstractElement wrappedElement = artElement.getAbstractElementOnArtNode();
    
    // TODO this is ugly, perhaps introduce AbstractElement.isBottom() instead?
    StopOperator stopOp = wrappedCpa.getStopOperator();
    if(stopOp instanceof CompositeStopOperator){
      CompositeStopOperator compStopOp = (CompositeStopOperator) stopOp;
      if(compStopOp.containsBottomElement(wrappedElement)){
        artElement.setBottom(true);
        return true;
      }
    }

    for (AbstractElement reachedElement : pReached) {
      ARTElement artReachedElement = (ARTElement)reachedElement;
      if (stop(artElement, artReachedElement)) {
        return true;
      }
    }
    return false;

  }

  private boolean stop(ARTElement pElement, ARTElement pReachedElement)
                                                      throws CPAException {

    AbstractElement wrappedElement = pElement.getAbstractElementOnArtNode();
    AbstractElement wrappedReachedElement = pReachedElement.getAbstractElementOnArtNode();

    StopOperator stopOp = wrappedCpa.getStopOperator();
    boolean stop = stopOp.stop(wrappedElement, wrappedReachedElement); 
    
    if (stop) {
      if (pElement.getMergedWith() == pReachedElement) {
        pElement.removeFromART();
      } else {
        pElement.setCovered(pReachedElement);
      }
    }
    return stop; 
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    return stop((ARTElement)pElement, (ARTElement)pReachedElement);
  }
}
