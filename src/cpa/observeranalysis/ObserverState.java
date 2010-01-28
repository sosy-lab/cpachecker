package cpa.observeranalysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;

/**
 * This class combines a ObserverInternal State with a variable Configuration.
 * Instaces of this class are passed to the CPAchecker as AbstractElement.
 * @author rhein
 */
class ObserverState implements AbstractElement {
  static final ObserverState TOP = new ObserverState();
  static final ObserverState BOTTOM = new ObserverState();
  
  private Map<String, ObserverVariable> vars;
  private ObserverInternalState internalState;
  
  private ObserverState() {
    vars = null;
    internalState = null;
  }
  
  ObserverState(Map<String, ObserverVariable> pVars,
      ObserverInternalState pInternalState) {
    super();
    vars = pVars;
    internalState = pInternalState;
  }

  ObserverState getFollowState (CFAEdge pCfaEdge) {
    if (this == TOP) return this;
    if (this == BOTTOM) return this;
    if (this.isError()) return this;
    // a new Set of Variables for the next state
    Map<String, ObserverVariable> newVars = null;
    ObserverInternalState followState = null;
    for (ObserverTransition t : internalState.getTransitions()) {
      if (t.match(pCfaEdge)) {
        if (t.assertionsHold(vars)) {
          // this transition will be taken. copy the variables
          newVars = deepClone(vars);
          t.executeActions(newVars);
          followState = t.getFollowState();
        } else {
          followState = ObserverInternalState.ERR;
          newVars = Collections.emptyMap();
        }
        break;
      }
    }
    /* if (followState!= sourceState) {
      System.out.println("Transition from " + sourceState.toString() + " to " + followState.toString());
    } */
    if (followState == null) {
      // no transition was taken, staying in the same state
      return this;
    } else {
      return new ObserverState(newVars, followState);
    }
  }
  
  private Map<String, ObserverVariable> deepClone (Map<String, ObserverVariable> pOld) {
    Map<String, ObserverVariable> result = new HashMap<String, ObserverVariable>(pOld.size());
    for (Entry<String, ObserverVariable> e : pOld.entrySet()) {
      result.put(e.getKey(), e.getValue().clone());
    }
    return result;
  }


  @Override
  public boolean isError() {
    if (this==TOP || this == BOTTOM) return false;
    return internalState == ObserverInternalState.ERR;
  }
  
  @Override
  public boolean equals(Object pObj) {
    if (super.equals(pObj)) {
      return true;
    }
    /* If one of the states is top or bottom they cannot be equal, Object.equal would have found this.
     * Because TOP and Bottom do not have internal States this must be returned explicitly.
     */
    if (this==TOP || this == BOTTOM) return false;
    if (!(pObj instanceof ObserverState)) {
      return false;
    }
    ObserverState otherState = (ObserverState) pObj;
    if (! this.internalState.equals(otherState.internalState)) {
      return false;
    }
    if (this.vars.equals(otherState.vars)) {
      return true;
    } else {
      return false;
    }
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   * 
   * I don't use the hashCode, but the method should be redefined every time equals is overwritten.
   */
  @Override
  public int hashCode() {
    if (this == TOP || this == BOTTOM) return super.hashCode();
    return this.internalState.hashCode() + this.vars.hashCode();
  }
  
  @Override
  public String toString() {
    StringBuffer v = new StringBuffer();
    for (ObserverVariable o : vars.values()) {
      v.append(' ');
      v.append(o.getName());
      v.append('=');
      v.append(o.getValue());
      v.append(' ');
    }
    return this.internalState.getName() + v;
  }

}
