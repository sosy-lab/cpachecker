/**
 * 
 */
package cpa.scoperestriction;

import java.util.HashMap;
import java.util.Map;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.automaton.Label;

/**
 * @author holzera
 *
 */
public class ScopeRestrictionAutomatonState implements AbstractElement {

  private Map<Label<CFAEdge>, ScopeRestrictionAutomatonState> mOutgoingTransitions;
  private ScopeRestrictionAutomatonCPA mCPA;
  
  public ScopeRestrictionAutomatonState(ScopeRestrictionAutomatonCPA pCPA) {
    assert(pCPA != null);
    
    mOutgoingTransitions = new HashMap<Label<CFAEdge>, ScopeRestrictionAutomatonState>();
    mCPA = pCPA;
  }
  
  public boolean isFinal() {
    return mCPA.isFinal(this);
  }
  
  public void addTransition(ScopeRestrictionAutomatonState pQ2,
                            Label<CFAEdge> pLabel) {
    assert(pQ2 != null);
    assert(pLabel != null);
    
    mOutgoingTransitions.put(pLabel, pQ2);
  }
  
  public AbstractElement getSuccessor(CFAEdge pEdge) {
    assert(pEdge != null);
    
    for (Map.Entry<Label<CFAEdge>, ScopeRestrictionAutomatonState> lEntry : mOutgoingTransitions.entrySet()) {
      if (lEntry.getKey().matches(pEdge)) {
        // we assume a deterministic behavior, so we return immediately
        return lEntry.getValue();
      }
    }
    
    return mCPA.getAbstractDomain().getBottomElement();
  }
  
}
