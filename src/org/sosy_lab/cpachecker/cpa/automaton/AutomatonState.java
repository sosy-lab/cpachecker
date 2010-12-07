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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collections;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import com.google.common.base.Joiner;

/**
 * This class combines a AutomatonInternal State with a variable Configuration.
 * Instaces of this class are passed to the CPAchecker as AbstractElement.
 * @author rhein
 */
class AutomatonState implements AbstractQueryableElement, Targetable {
  private static final String AutomatonAnalysisNamePrefix = "AutomatonAnalysis_";

  static class TOP extends AutomatonState {
    public TOP(ControlAutomatonCPA pAutomatonCPA) {
      super(pAutomatonCPA);
    }
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == top");
    }
    @Override
    public String toString() {
      return "AutomatonState.TOP";
    }
  }
  static class BOTTOM extends AutomatonState {
    public BOTTOM(ControlAutomatonCPA pAutomatonCPA) {
      super(pAutomatonCPA);
    }
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == bottom");
    }
    @Override
    public String toString() {
      return "AutomatonState.BOTTOM";
    }
  }

  private final ControlAutomatonCPA automatonCPA;
  private final Map<String, AutomatonVariable> vars;
  private final AutomatonInternalState internalState;


  static AutomatonState automatonStateFactory(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA) {
    if (pInternalState == AutomatonInternalState.BOTTOM) {
      return pAutomatonCPA.getBottomState();
    } else {
      return new AutomatonState(pVars, pInternalState, pAutomatonCPA);
    }
  }

  private AutomatonState(ControlAutomatonCPA pAutomatonCPA) {
    automatonCPA = pAutomatonCPA;
    vars = Collections.emptyMap();
    internalState = null;
  }

  private AutomatonState(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA) {
    vars = pVars;
    internalState = pInternalState;
    automatonCPA = pAutomatonCPA;
  }

  @Override
  public boolean isTarget() {
    return (internalState != null) && internalState.isTarget();
  }

  @Override
  public boolean equals(Object pObj) {
    if (super.equals(pObj)) {
      return true;
    }
    if (!(pObj instanceof AutomatonState)) {
      return false;
    }
    AutomatonState otherState = (AutomatonState) pObj;

    /* If one of the states is top or bottom they cannot be equal, Object.equal would have found this.
     * Because TOP and Bottom do not have internal States this must be returned explicitly.
     * AutomatonUnknownState also have internalState==null,
     * and are only equal to one of themselves.
     */
    if (this.internalState == null || otherState.internalState == null) {
      return false;
    }
    
    return this.internalState.equals(otherState.internalState)
        && this.vars.equals(otherState.vars);
  }

  @Override
  public int hashCode() {
    return (internalState == null) ? super.hashCode()
      : (internalState.hashCode() * 17 + vars.hashCode());
  }

  @Override
  public String toString() {
    return internalState.getName() + ' ' + Joiner.on(' ').withKeyValueSeparator("=").join(vars);
  }


  /**
   * The UnknownState represents one of the States following a normal State of the Automaton.
   * Which State is the correct following state could not be determined so far.
   * This Class is used if during a "getAbstractSuccessor" call the abstract successor could not be determined.
   * During the subsequent "strengthen" call enough information should be available to determine a normal AutomatonState as following State.
   * @author rhein
   *
   */
  static class AutomatonUnknownState extends AutomatonState {
    private final AutomatonState previousState;

    AutomatonUnknownState(AutomatonState pPreviousState) {
      super(pPreviousState.automatonCPA);
      previousState = pPreviousState;
    }

    AutomatonState getPreviousState() {
      return previousState;
    }
    @Override
    AutomatonInternalState getInternalState() {
      return previousState.getInternalState();
    }
    @Override
    Map<String, AutomatonVariable> getVars() {
      return previousState.getVars();
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (!(pObj instanceof AutomatonUnknownState)) {
        return false;
      }
      AutomatonUnknownState otherState = (AutomatonUnknownState) pObj;
      return previousState.equals(otherState.previousState);
    }

    @Override
    public int hashCode() {
      return this.previousState.hashCode() + 724;
    }

    @Override
    public String toString() {
      return "AutomatonUnknownState<" + previousState.toString() + ">";
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // e.g. "state == name-of-state" where name-of state can be top, bottom, error, or any state defined in the automaton definition.
    String[] parts = pProperty.split("==");
    if (parts.length != 2)
      throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
    else {
      if (parts[0].trim().toLowerCase().equals("state")) {
        return this.getInternalState().getName().equals(parts[1].trim());
      } else {
        if (this.vars.containsKey(parts[0].trim())) {
          // is a local variable
          try {
            int val = Integer.parseInt(parts[1]);
            return vars.get(parts[0]).getValue() == val;
          } catch (NumberFormatException e) {
            throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not parse the int \"" + parts[1] + "\".");
          }
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Only accepting \"State == something\" and \"varname = something\" queries so far.");
        }
      }
    }
  }
  
  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // allows to set values of Automaton variables like "x:=6"
    String[] parts = pModification.split(":=");
    if (parts.length != 2)
      throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not split the string correctly.");
    else {
      AutomatonVariable var = this.vars.get(parts[0].trim());
      if (var != null) {
        try {
          int val = Integer.parseInt(parts[1]);
          var.setValue(val);
        } catch (NumberFormatException e) {
          throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not parse the int \"" + parts[1].trim() + "\".");
        }
      } else {
        throw new InvalidQueryException("Could not modify the variable \"" + parts[0].trim() + "\" (Variable not found)");
      }
    }
  }

  @Override
  public Boolean evaluateProperty(String pProperty) throws InvalidQueryException {
    return Boolean.valueOf(checkProperty(pProperty));
  }

  @Override
  public String getCPAName() {
    return AutomatonState.AutomatonAnalysisNamePrefix + automatonCPA.getAutomaton().getName();
  }

  AutomatonInternalState getInternalState() {
    return internalState;
  }

  Map<String, AutomatonVariable> getVars() {
   return vars;
  }
}
