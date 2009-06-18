package cpa.common.interfaces;

import java.util.Collection;

public interface RefinementManager {
  
  boolean performRefinement(Collection<AbstractElementWithLocation> pReached);

}
