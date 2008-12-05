/**
 * 
 */
package cpa.scoperestrictionautomaton;

import java.util.HashMap;
import java.util.Map;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.scoperestrictionautomaton.label.Label;

/**
 * @author holzera
 *
 */
public class ScopeRestrictionAutomatonState implements AbstractElement {

  private Map<Label<CFAEdge>, ScopeRestrictionAutomatonState> mOutgoingTransitions;
  private ScopeRestrictionAutomatonCPA mCPA;
  
  public ScopeRestrictionAutomatonState(ScopeRestrictionAutomatonCPA pCPA) {
    if (pCPA == null) {
      throw new IllegalArgumentException("Given scope-restriction cpa is null!");
    }
    
    mOutgoingTransitions = new HashMap<Label<CFAEdge>, ScopeRestrictionAutomatonState>();
    mCPA = pCPA;
  }
  
  public boolean isFinal() {
    return mCPA.isFinal(this);
  }
  
  public void addTransition(ScopeRestrictionAutomatonState pQ2,
                            Label<CFAEdge> pLabel) {
    if (pQ2 == null) {
      throw new IllegalArgumentException("Given automaton state is null!");
    }
    
    if (pLabel == null) {
      throw new IllegalArgumentException("Given label is null!");
    }
    
    mOutgoingTransitions.put(pLabel, pQ2);
  }
  
  public AbstractElement getSuccessor(CFAEdge pEdge) {
    if (pEdge == null) {
      throw new IllegalArgumentException("Given CFAEdge is null!");
    }
    
    for (Map.Entry<Label<CFAEdge>, ScopeRestrictionAutomatonState> lEntry : mOutgoingTransitions.entrySet()) {
      if (lEntry.getKey().matches(pEdge)) {
        // we assume a deterministic behavior, so we return immediately
        return lEntry.getValue();
      }
    }
    
    return mCPA.getAbstractDomain().getBottomElement();
  }
  
}
