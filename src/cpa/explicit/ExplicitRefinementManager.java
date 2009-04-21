package cpa.explicit;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.RefinementManager;

public class ExplicitRefinementManager implements RefinementManager {

  @Override
  public boolean performRefinement(AbstractElement pElement) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean performRefinement(AbstractElement pElement,
      ARTElement pARTElement) {
    System.out.println(pARTElement.pathToString());
    return true;
  }

}
