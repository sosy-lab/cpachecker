package cpa.common.interfaces;

import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;

public interface RefinementManager {
  
  RefinementOutcome performRefinement(ReachedElements pReached, Path pPath);

}
