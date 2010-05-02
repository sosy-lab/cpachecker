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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  
  // the pattern \$\$\d+ matches Expressions like $$x $$y23rinnksd $$observerVar (all terminated by a non-word-character)
  static Pattern OBSERVER_VARS_PATTERN = Pattern.compile("\\$\\$[a-zA-Z]\\w*");
  // the pattern \$\d+ matches Expressions like $1 $2 $3 $201
  // If this pattern is changed the pattern in ObserverASTcomparison should be changed too!
  static Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");

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
  public void clearLogMessage() {
    transitionLogMessages = "";
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

  /**
   * This method replaces all references to
   * 1. ObserverVariables (referenced by $$<Name-of-Variable>)
   * 2. TransitionVariables (referenced by $<Number-of-Variable>)
   * with the values of the variables.
   * If the variable is not found the function returns null.
   * @param pSourceString
   * @return
   */
  String replaceVariables (String pSourceString) {
  
    // replace references to Transition Variables
    Matcher matcher = ObserverExpressionArguments.TRANSITION_VARS_PATTERN.matcher(pSourceString);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(result, "");
      String key = matcher.group().substring(1); // matched string startswith $
      try {
        int varKey = Integer.parseInt(key);
        String var = this.getTransitionVariable(varKey);
        if (var == null) {
          // this variable has not been set.
          this.getLogger().log(Level.WARNING, "could not replace the transition variable $" + varKey + " (not found).");
          return null;
        } else {
          result.append(var);
        }
      } catch (NumberFormatException e) {
        this.getLogger().log(Level.WARNING, "could not parse the int in " + matcher.group() + " , leaving it untouched");
        result.append(matcher.group());
      }
    }
    matcher.appendTail(result);
  
    // replace references to observer Variables
    matcher = ObserverExpressionArguments.OBSERVER_VARS_PATTERN.matcher(result.toString());
    result = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(result, "");
      String varName =  matcher.group().substring(2); // matched string starts with $$
      ObserverVariable variable = this.getObserverVariables().get(varName);
      if (variable == null) {
        // this variable has not been set.
        this.getLogger().log(Level.WARNING, "could not replace the Observer variable reference " + varName + " (not found).");
        return null;
      } else {
        result.append(variable.getValue());
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
