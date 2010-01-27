package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import cpa.common.interfaces.AbstractElement;

/** Represents a State in the observer automaton.
 * @author rhein
 */
class ObserverInternalState {
  static final List<ObserverTransition> emptyTransitionList = Collections.emptyList();
  /** Error State */
  static final ObserverInternalState ERR = new ObserverInternalState("ERR", emptyTransitionList);
  
  // the StateId is used to identify States in GraphViz
  private static int stateIdCounter = 0;
  // stateIdCounter is incremented every time an instance of ObserverState is created.
  private int stateId = stateIdCounter++;
  
  /** Name of this State.  */
  private String name;
  /** Outgoing transitions of this state.  */
  private List<ObserverTransition> transitions;
  
  public ObserverInternalState(String pName, List<ObserverTransition> pTransitions) {
    this.name = pName;
    this.transitions = pTransitions;
  }
  
  /** Lets all outgoing transitions of this state resolve their "sink" states.
   * @param pAllStates map of all states of this automaton.
   */
  void setFollowStates(List<ObserverInternalState> pAllStates) {
    for (ObserverTransition t : transitions) {
      t.setFollowState(pAllStates);
    }
  }

  public String getName() {
    return name;
  }
  /** @return a integer representation of this state.
   */
  public int getStateId() {
    return stateId;
  }


  /**  Writes a representation of this state (as node) in DOT file format to the argument {@link PrintStream}.
   * @param pOut
   */
  public void writeTransitionsToDotFile(PrintStream pOut) {
    for (ObserverTransition t : transitions) {
      t.writeTransitionToDotFile(stateId, pOut);
    }
  }

  public List<ObserverTransition> getTransitions() {
    return transitions;
  }
  
  @Override
  public String toString() {
    return this.name;
  }
}
