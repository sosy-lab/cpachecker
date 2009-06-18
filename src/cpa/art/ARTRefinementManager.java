package cpa.art;

import java.util.Collection;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.RefinementManager;

public class ARTRefinementManager implements RefinementManager {
  
  ConfigurableProgramAnalysis wrappedCpa;

  public ARTRefinementManager(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
  }

  @Override
  public boolean performRefinement(
      Collection<AbstractElementWithLocation> pReached) {
    AbstractElement lastElementAdded = ....;
    ARTElement lastArtElement = (ARTElement)lastElementAdded;
    
  }

  
  
}
