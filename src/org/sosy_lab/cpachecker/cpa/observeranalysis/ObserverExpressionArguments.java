package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class ObserverExpressionArguments {
  private Map<String, ObserverVariable> observerVariables;
  // Variables that are only valid for one transition ($1,$2,...)
  // these will be set in a MATCH statement, and are erased when the transitions actions are executed.
  private Map<Integer, String> transitionVariables = new HashMap<Integer, String>();
  private List<AbstractElement> abstractElements;
  private CFAEdge cfaEdge;
  private LogManager logger;
  /**
   * In this String all print messages of the Transition are collected.
   * They are logged (INFO-level) together at the end of the transition actions.
   */
  private String transitionLogMessages = "";

  ObserverExpressionArguments(Map<String, ObserverVariable> pObserverVariables,
      List<AbstractElement> pAbstractElements, CFAEdge pCfaEdge, LogManager pLogger) {
    super();
    if (pObserverVariables == null)
      observerVariables = Collections.emptyMap();
    else
      observerVariables = pObserverVariables;
    if (pAbstractElements == null)
      abstractElements = Collections.emptyList();
    else 
      abstractElements = pAbstractElements;
    cfaEdge = pCfaEdge;
    logger = pLogger;
  }

  void setObserverVariables(Map<String, ObserverVariable> pObserverVariables) {
    observerVariables = pObserverVariables;
  }

  Map<String, ObserverVariable> getObserverVariables() {
    return observerVariables;
  }

  List<AbstractElement> getAbstractElements() {
    return abstractElements;
  }

  CFAEdge getCfaEdge() {
    return cfaEdge;
  }
  
  LogManager getLogger() {
    return logger;
  }
  void appendToLogMessage(String message) {
    this.transitionLogMessages = transitionLogMessages  + message;
  }
  void appendToLogMessage(int message) {
    this.transitionLogMessages = transitionLogMessages  + message;
  }
  String getLogMessage() {
    return transitionLogMessages;
  }
  
  void clearTransitionVariables() {
    this.transitionVariables.clear();
  }
  String getTransitionVariable(int key) {
    // this is the variable adressed with $<key> in the observer automaton
    return this.transitionVariables.get(Integer.valueOf(key));
  }
  
  void putTransitionVariable(int key, String value) {
    this.transitionVariables.put(key, value);
  }
}
