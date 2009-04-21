package cpa.location;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.RefinementManager;

public class LocationRefinementManager implements RefinementManager {

  @Override
  public boolean performRefinement(AbstractElement pElement) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean performRefinement(AbstractElement pElement,
      ARTElement pARTElement) {
    return true;
  }

}
