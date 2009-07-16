package cpa.explicit;

import cpa.art.Path;
import cpa.common.CPAAlgorithm;
import cpa.common.ReachedElements;
import cpa.common.interfaces.RefinementManager;

public class ExplicitRefinementManager implements RefinementManager {

  @Override
  public boolean performRefinement(ReachedElements pReached, Path pPath) {
    // we do not perform refinement
    // we just report an error
    CPAAlgorithm.errorFound = true;
    System.out.println(pPath);
    return false;
  }
  
}
