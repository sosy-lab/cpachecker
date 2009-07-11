package cpa.common.interfaces;

import cpa.art.Path;
import cpa.common.ReachedElements;

public interface RefinementManager {
  
  boolean performRefinement(ReachedElements pReached);
  boolean performRefinement(Path path);

}
