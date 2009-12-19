package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import cpa.common.interfaces.AbstractElement;

class ObserverState implements AbstractElement {
  static final List<ObserverTransition> emptyTransitionList = Collections.emptyList();
  static final ObserverState ERR = new ObserverState("ERR", emptyTransitionList);
  static final ObserverState TOP = new ObserverState("TOP", emptyTransitionList);
  static final ObserverState BOTTOM = new ObserverState("BOTTOM", emptyTransitionList);
  
  // the StateId is used to identify States in GraphViz
  private static int stateIdCounter = 0;
  private int stateId = stateIdCounter++;
  
  private String name;
  private List<ObserverTransition> transitions;
  
  public ObserverState(String name, List<ObserverTransition> transitions) {
    this.name = name;
    this.transitions = transitions;
  }
  
  void setFollowStates(List<ObserverState> allStates) {
    for (ObserverTransition t : transitions) {
      t.setFollowState(allStates);
    }
  }

  public String getName() {
    return name;
  }
  public int getStateId() {
    return stateId;
  }

  public void writeTransitionsToDotFile(PrintStream out) {
    for (ObserverTransition t : transitions) {
      t.writeTransitionToDotFile(stateId, out);
    }
  }

  @Override
  public boolean isError() {
    return this.equals(ERR);
  }

  public List<ObserverTransition> getTransitions() {
    return transitions;
  }
  
  @Override
  public String toString() {
    return this.name;
  }
}
