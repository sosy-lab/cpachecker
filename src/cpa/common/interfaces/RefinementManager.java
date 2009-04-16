package cpa.common.interfaces;

import cpa.art.ARTElement;

public interface RefinementManager {
  
  boolean performRefinement(AbstractElement pElement);
  boolean performRefinement(AbstractElement pElement, ARTElement pARTElement);

}
