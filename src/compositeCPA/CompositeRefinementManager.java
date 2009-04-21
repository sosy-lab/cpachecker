package compositeCPA;

import java.util.List;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.RefinementManager;

public class CompositeRefinementManager implements RefinementManager{

  private final List<RefinementManager> refinementManagers;
  
  public CompositeRefinementManager(List<RefinementManager> pRefinementManagers) {
    refinementManagers = pRefinementManagers;
  }

  @Override
  public boolean performRefinement(AbstractElement pElement) {
    // TODO throw an exception here
    return false;
  }

  @Override
  public boolean performRefinement(AbstractElement pElement,
      ARTElement pARTElement) {
    CompositeElement compositeElement = (CompositeElement) pElement;
    boolean stopAnalysis = true;
    for(int i=0; i<refinementManagers.size(); i++){
      RefinementManager refMan = refinementManagers.get(i);
      AbstractElement abstElem = compositeElement.get(i);
      stopAnalysis = stopAnalysis && refMan.performRefinement(abstElem, pARTElement);
    }
    return stopAnalysis;
  }
}
