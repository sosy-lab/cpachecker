/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
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
