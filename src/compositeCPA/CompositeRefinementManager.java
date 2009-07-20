package compositeCPA;

import java.util.List;

import cpa.art.Path;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.RefinementManager;

public class CompositeRefinementManager implements RefinementManager{

  private final List<RefinementManager> refinementManagers;
  
  public CompositeRefinementManager(List<RefinementManager> pRefinementManagers) {
    refinementManagers = pRefinementManagers;
  }

  @Override
  public RefinementOutcome performRefinement(ReachedElements pReached, Path pPath) {
    
    for(RefinementManager rm: refinementManagers){
      RefinementOutcome output = rm.performRefinement(pReached, pPath); 
      if(output.refinementPerformed()){
        return output;
      }
    }
    return new RefinementOutcome();
  }
}
