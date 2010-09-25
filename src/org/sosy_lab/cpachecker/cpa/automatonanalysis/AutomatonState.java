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
package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * This class combines a AutomatonInternal State with a variable Configuration.
 * Instaces of this class are passed to the CPAchecker as AbstractElement.
 * @author rhein
 */
class AutomatonState implements AbstractQueryableElement, Targetable {
  private static final String AutomatonAnalysisNamePrefix = "AutomatonAnalysis_";

  static class TOP extends AutomatonState {
    public TOP(ControlAutomatonCPA pAutomatonCPA) {
      super();
      super.automatonCPA = pAutomatonCPA;
    }
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == top");
    }
    @Override
    public String getCPAName() {
      return AutomatonAnalysisNamePrefix +  getAutomatonCPA().getAutomaton().getName();
    }
    @Override
    public boolean isTarget() {
      return false;
    }
  }
  static class BOTTOM extends AutomatonState {
    public BOTTOM(ControlAutomatonCPA pAutomatonCPA) {
      super();
      super.automatonCPA = pAutomatonCPA;
    }
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == bottom");
    }
    @Override
    public String getCPAName() {
      return AutomatonAnalysisNamePrefix +  getAutomatonCPA().getAutomaton().getName();
    }
    @Override
    public boolean isTarget() {
      return false;
    }
  }

  private ControlAutomatonCPA automatonCPA;
  private Map<String, AutomatonVariable> vars;
  private AutomatonInternalState internalState;


  static AutomatonState automatonStateFactory(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA) {
    if (pInternalState == AutomatonInternalState.BOTTOM) {
      return pAutomatonCPA.getBottomState();
    } else {
      return new AutomatonState(pVars, pInternalState, pAutomatonCPA);
    }
  }

  private AutomatonState() {
    automatonCPA = null;
    vars = null;
    internalState = null;
  }

  private AutomatonState(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA) {
    super();
    vars = pVars;
    internalState = pInternalState;
    this.automatonCPA = pAutomatonCPA;
  }

  @Override
  public boolean isTarget() {
    if (this==this.automatonCPA.getTopState() || this == this.automatonCPA.getBottomState()) return false;
    return internalState == AutomatonInternalState.ERROR;
  }

  @Override
  public boolean equals(Object pObj) {
    if (super.equals(pObj)) {
      return true;
    }

    if (pObj == null) {
      return false;
    }

    /* If one of the states is top or bottom they cannot be equal, Object.equal would have found this.
     * Because TOP and Bottom do not have internal States this must be returned explicitly.
     */
    if (this == getAutomatonCPA().getTopState()
        || this == getAutomatonCPA().getBottomState()
        || pObj == getAutomatonCPA().getTopState()
        || pObj == getAutomatonCPA().getBottomState()) return false;
    if (!(pObj instanceof AutomatonState)) {
      return false;
    }
    AutomatonState otherState = (AutomatonState) pObj;
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
    if (this == getAutomatonCPA().getTopState() || this == getAutomatonCPA().getBottomState()) return super.hashCode();
    return this.internalState.hashCode() + this.vars.hashCode();
  }

  @Override
  public String toString() {
    if (this == getAutomatonCPA().getTopState()) return "AutomatonState.TOP";
    if (this == getAutomatonCPA().getBottomState()) return "AutomatonState.BOTTOM";
    StringBuilder v = new StringBuilder();
    for (AutomatonVariable o : vars.values()) {
      v.append(' ');
      v.append(o.getName());
      v.append('=');
      v.append(o.getValue());
      v.append(' ');
    }
    return this.internalState.getName() + v;
  }

  protected ControlAutomatonCPA getAutomatonCPA() {
    return this.automatonCPA;
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
    private AutomatonState previousState;

    AutomatonUnknownState(AutomatonState pPreviousState) {
      super();
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
    public boolean isTarget() {
      return false;
    }

    @Override
    public boolean equals(Object pObj) {
      if (super.equals(pObj)) {
        return true;
      }
      if (!(pObj instanceof AutomatonUnknownState)) {
        return false;
      }
      AutomatonUnknownState otherState = (AutomatonUnknownState) pObj;
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

      return "AutomatonUnknownState<" + previousState.toString() + ">";
    }

    @Override
    protected ControlAutomatonCPA getAutomatonCPA() {
      return previousState.getAutomatonCPA();
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
  public void modifyProperty(String pModification)
  // allows to set values of Automaton variables like "x:=6"
      throws InvalidQueryException {
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
  public Boolean evaluateProperty(
      String pProperty) throws InvalidQueryException {
    return Boolean.valueOf(checkProperty(pProperty));
  }
  @Override
  public String getCPAName() {
    return AutomatonState.AutomatonAnalysisNamePrefix + this.getAutomatonCPA().getAutomaton().getName();
  }

  AutomatonInternalState getInternalState() {
    return this.internalState;
  }

  Map<String, AutomatonVariable> getVars() {
   return vars;
  }
}
