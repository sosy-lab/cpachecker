package cpa.common.interfaces;

import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import exceptions.CPAException;

public interface Refiner {
  
  public RefinementOutcome performRefinement(ReachedElements pReached) throws CPAException;

}
