package cpa.common.interfaces;

import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;

public interface Refiner {
  
  public RefinementOutcome performRefinement(ReachedElements pReached);

}
