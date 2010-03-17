package cpa.observeranalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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

  /**
   * Returns the <code>ObserverState</code> that follows this State in the ObserverAutomatonCPA.
   * If the passed <code>ObserverExpressionArguments</code> are not sufficient to determine the following state
   * this method returns a <code>ObserverUnknownState</code> that contains this as previous State.
   * The strengthen method of the <code>ObserverUnknownState</code> should be used once enough Information is available to determine the correct following State.
   * @param exprArgs
   * @return
   */
  ObserverState getFollowState (ObserverExpressionArguments exprArgs) {
    if (this == TOP) return this;
    if (this == BOTTOM) return this;
    if (this.isError()) return this;
    // this variable will be returned, default return value is this.
    ObserverState returnState = this;
    // a new Set of Variables for the next state
    Map<String, ObserverVariable> newVars = null;
    ObserverInternalState followState = null;
    exprArgs.setObserverVariables(vars);
    //ObserverExpressionArguments exprArgs = new ObserverExpressionArguments(vars, null, pCfaEdge);
    for (ObserverTransition t : internalState.getTransitions()) {
      boolean exitLoop = false;
      switch (t.match(exprArgs)) { 
      case TRUE :
        if (t.assertionsHold(exprArgs)) {
          // this transition will be taken. copy the variables
          newVars = deepClone(vars);
          exprArgs.setObserverVariables(newVars);
          t.executeActions(exprArgs);
          followState = t.getFollowState();
        } else {
          followState = ObserverInternalState.ERR;
          newVars = Collections.emptyMap();
        }
        returnState = new ObserverState(newVars, followState);
        exitLoop = true;
        break;
      case MAYBE : 
        // if one transition cannot be evaluated the evaluation must be postponed until enough information is available
        returnState = new ObserverUnknownState(this);
        exitLoop = true;
        break;
      case FALSE :
      default :
        // consider next transition
      }
      if (exitLoop) {
        break;
      }
    }
    return returnState;
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

  
  /**
   * The UnknownState represents one of the States following a normal State of the ObserverAutomaton.
   * Which State is the correct following state could not be determined so far.
   * This Class is used if during a "getAbstractSuccessor" call the abstract successor could not be determined.
   * During the subsequent "strengthen" call enough information should be available to determine a normal ObserverState as following State. 
   * @author rhein
   *
   */
  static class ObserverUnknownState extends ObserverState{
    private ObserverState previousState;

    ObserverUnknownState(ObserverState pPreviousState) {
      super();
      previousState = pPreviousState;
    }

    @Override
    public boolean isError() {
      return false;
    }

    public Collection<? extends AbstractElement> strengthen(ObserverExpressionArguments exprArgs) {
      exprArgs.setObserverVariables(previousState.vars);
      ObserverState ret = previousState.getFollowState(exprArgs);
      if (ret instanceof ObserverUnknownState) {
        // FEHLER nicht genügend informationen um nächsten State zu bestimmen!
        return Collections.singleton(TOP);
      } else {
        return Collections.singleton(ret);
      }
    }
    @Override
    public boolean equals(Object pObj) {
      if (super.equals(pObj)) {
        return true;
      }
      if (!(pObj instanceof ObserverUnknownState)) {
        return false;
      }
      ObserverUnknownState otherState = (ObserverUnknownState) pObj;
      if (this.previousState.equals(otherState.previousState)) {
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
      return this.previousState.hashCode() + 724;
    }
    
    @Override
    public String toString() {
      
      return "ObserverUnknownState<" + previousState.toString() + ">";
    }
  }
}
