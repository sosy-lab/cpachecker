/**
 * 
 */
package cpa.scoperestriction;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.TopElement;
import cpa.common.automaton.Label;

/**
 * @author holzera
 *
 */
public class TopElementScopeRestrictionAutomatonState extends
                                                     ScopeRestrictionAutomatonState
                                                                                   implements
                                                                                   TopElement {

  /**
   * @param pCPA
   */
  public TopElementScopeRestrictionAutomatonState(
                                                  ScopeRestrictionAutomatonCPA pCPA) {
    super(pCPA);
  }

  @Override
  public void addTransition(ScopeRestrictionAutomatonState pQ2,
                            Label<CFAEdge> pLabel) {
 // just do nothing here since we always return \top in getSuccessor().
  }

  @Override
  public AbstractElement getSuccessor(CFAEdge pEdge) {
    return this;
  }

  @Override
  public boolean equals(Object pOther) {
    return (pOther instanceof TopElementScopeRestrictionAutomatonState);
  }
  
  @Override
  public int hashCode() {
    return Integer.MAX_VALUE;
  }
  
  @Override
  public String toString() {
    return "\top";
  }
}
