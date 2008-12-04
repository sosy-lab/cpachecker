/**
 * 
 */
package cpa.scoperestrictionautomaton;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.scoperestrictionautomaton.label.Label;

/**
 * @author holzera
 *
 */
public class BottomElementScopeRestrictionAutomatonState extends
                                                        ScopeRestrictionAutomatonState
                                                                                      implements
                                                                                      BottomElement {

  /**
   * @param pCPA
   */
  public BottomElementScopeRestrictionAutomatonState(ScopeRestrictionAutomatonCPA pCPA) {
    super(pCPA);
  }

  @Override
  public void addTransition(ScopeRestrictionAutomatonState pQ2,
                            Label<CFAEdge> pLabel) {
    // just do nothing here since we always return \bot in getSuccessor().
  }

  @Override
  public AbstractElement getSuccessor(CFAEdge pEdge) {
    return this;
  }

  @Override
  public boolean equals(Object pOther) {
    return (pOther instanceof BottomElementScopeRestrictionAutomatonState);
  }
  
  @Override
  public int hashCode() {
    return Integer.MIN_VALUE;
  }
  
  @Override
  public String toString() {
    return "\bot";
  }
}
