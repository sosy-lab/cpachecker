package cpa.observeranalysis;

import java.util.Map;

import cpa.common.interfaces.AbstractElement;

/**
 * This class combines a ObserverInternal State with a variable Configuration.
 * Instaces of this class are passed to the CPAchecker as AbstractElement.
 * @author rhein
 */
class ObserverState implements AbstractElement {

  static final ObserverState TOP = new ObserverState();
  static final ObserverState BOTTOM = new ObserverState();
  
  private final Map<String, ObserverVariable> vars;
  private final ObserverInternalState internalState;
  
  static ObserverState observerStateFactory(Map<String, ObserverVariable> pVars,
      ObserverInternalState pInternalState) {
    if (pInternalState == ObserverInternalState.BOTTOM) {
      return BOTTOM;
    } else {
      return new ObserverState(pVars, pInternalState);
    }
  }
  
  private ObserverState() {
    vars = null;
    internalState = null;
  }
  
  private ObserverState(Map<String, ObserverVariable> pVars,
      ObserverInternalState pInternalState) {

    vars = pVars;
    internalState = pInternalState;
  }

  public ObserverInternalState getInternalState() {
    return internalState;
  }
  
  public Map<String, ObserverVariable> getVars() {
    return vars;
  }
  
  @Override
  public boolean isError() {
    if (this==TOP || this == BOTTOM) return false;
    return internalState == ObserverInternalState.ERROR;
  }
  
  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    
    if (pObj == null || !(pObj instanceof ObserverState)) {
      return false;
    }
    
    /* If one of the states is top or bottom they cannot be equal, Object.equal would have found this.
     * Because TOP and Bottom do not have internal States this must be returned explicitly.
     */
    if (this == TOP || this == BOTTOM || pObj == TOP || pObj == BOTTOM) return false;

    ObserverState otherState = (ObserverState) pObj;
    return this.internalState.equals(otherState.internalState)
        && this.vars.equals(otherState.vars);
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
    if (this == TOP) return "ObserverState.TOP";
    if (this == BOTTOM) return "ObserverState.BOTTOM";
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
    private final ObserverState previousState;

    ObserverUnknownState(ObserverState pPreviousState) {
      super();
      previousState = pPreviousState;
    }

    @Override
    public boolean isError() {
      return false;
    }
    
    @Override
    public ObserverInternalState getInternalState() {
      return previousState.getInternalState();
    }

    @Override
    public Map<String, ObserverVariable> getVars() {
      return previousState.getVars();
    }
    
    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj == null || !(pObj instanceof ObserverUnknownState)) {
        return false;
      }
      ObserverUnknownState otherState = (ObserverUnknownState) pObj;
      return this.previousState.equals(otherState.previousState);
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
