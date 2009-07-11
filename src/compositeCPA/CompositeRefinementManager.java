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
  public boolean performRefinement(ReachedElements pReached) {
    // TODO exception
    // this method is used by ARTCPA
    assert(false);
    return false;
  }

  @Override
  public boolean performRefinement(Path pPath) {
    
    for(RefinementManager rm: refinementManagers){
      if(rm.performRefinement(pPath)){
        return true;
      }
    }
    return false;
  }
  
}
