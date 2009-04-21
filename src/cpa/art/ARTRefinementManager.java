package cpa.art;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.RefinementManager;

public class ARTRefinementManager implements RefinementManager {
  
  ConfigurableProgramAnalysis cpa;

  public ARTRefinementManager(ConfigurableProgramAnalysis pWrappedCPA) {
    cpa = pWrappedCPA;
  }

  @Override
  public boolean performRefinement(AbstractElement element) {
    ARTElement artElement = (ARTElement) element;
    return performRefinement(element, artElement);
  }

  @Override
  public boolean performRefinement(AbstractElement pElement, ARTElement pARTElement) {
    AbstractElement wrappedElement = pARTElement.getAbstractElementOnArtNode();
    RefinementManager wrappedRefinementMan = cpa.getRefinementManager();
    return wrappedRefinementMan.performRefinement(wrappedElement, pARTElement);
  }

}
