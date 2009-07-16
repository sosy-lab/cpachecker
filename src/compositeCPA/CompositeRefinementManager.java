package compositeCPA;

import java.util.List;

import cpa.art.Path;
import cpa.common.ReachedElements;
import cpa.common.interfaces.RefinementManager;

public class CompositeRefinementManager implements RefinementManager{

  private final List<RefinementManager> refinementManagers;
  
  public CompositeRefinementManager(List<RefinementManager> pRefinementManagers) {
    refinementManagers = pRefinementManagers;
  }

  @Override
  public boolean performRefinement(ReachedElements pReached, Path pPath) {
    
    for(RefinementManager rm: refinementManagers){
      if(rm.performRefinement(pReached, pPath)){
        return true;
      }
    }
    return false;
  }
  
}
