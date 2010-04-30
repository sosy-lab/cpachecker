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

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * This class combines a ObserverInternal State with a variable Configuration.
 * Instaces of this class are passed to the CPAchecker as AbstractElement.
 * @author rhein
 */
class ObserverState implements AbstractQueryableElement {
  private static final String ObserverAnalysisNamePrefix = "ObserverAnalysis_";

  static class TOP extends ObserverState {
    public TOP(ObserverAutomatonCPA pAutomatonCPA) {
      super();
      super.automatonCPA = pAutomatonCPA;
    }
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == top");
    }
    @Override
    public String getCPAName() {
      return ObserverAnalysisNamePrefix +  getAutomatonCPA().getAutomaton().getName();
    }
    @Override
    public boolean isError() {
      return false;
    }
  }
  static class BOTTOM extends ObserverState {
    public BOTTOM(ObserverAutomatonCPA pAutomatonCPA) {
      super();
      super.automatonCPA = pAutomatonCPA;
    }
    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == bottom");
    }
    @Override
    public String getCPAName() {
      return ObserverAnalysisNamePrefix +  getAutomatonCPA().getAutomaton().getName();
    }
    @Override
    public boolean isError() {
      return false;
    }
  }

  private ObserverAutomatonCPA automatonCPA;
  private Map<String, ObserverVariable> vars;
  private ObserverInternalState internalState;


  static ObserverState observerStateFactory(Map<String, ObserverVariable> pVars,
      ObserverInternalState pInternalState, ObserverAutomatonCPA pAutomatonCPA) {
    if (pInternalState == ObserverInternalState.BOTTOM) {
      return pAutomatonCPA.getBottomState();
    } else {
      return new ObserverState(pVars, pInternalState, pAutomatonCPA);
    }
  }

  private ObserverState() {
    automatonCPA = null;
    vars = null;
    internalState = null;
  }

  private ObserverState(Map<String, ObserverVariable> pVars,
      ObserverInternalState pInternalState, ObserverAutomatonCPA pAutomatonCPA) {
    super();
    vars = pVars;
    internalState = pInternalState;
    this.automatonCPA = pAutomatonCPA;
  }

  @Override
  public boolean isError() {
    if (this==this.automatonCPA.getTopState() || this == this.automatonCPA.getBottomState()) return false;
    return internalState == ObserverInternalState.ERROR;
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
    if (this == getAutomatonCPA().getTopState() || this == getAutomatonCPA().getBottomState()) return super.hashCode();
    return this.internalState.hashCode() + this.vars.hashCode();
  }

  @Override
  public String toString() {
    if (this == getAutomatonCPA().getTopState()) return "ObserverState.TOP";
    if (this == getAutomatonCPA().getBottomState()) return "ObserverState.BOTTOM";
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

  protected ObserverAutomatonCPA getAutomatonCPA() {
    return this.automatonCPA;
  }


  /**
   * The UnknownState represents one of the States following a normal State of the ObserverAutomaton.
   * Which State is the correct following state could not be determined so far.
   * This Class is used if during a "getAbstractSuccessor" call the abstract successor could not be determined.
   * During the subsequent "strengthen" call enough information should be available to determine a normal ObserverState as following State.
   * @author rhein
   *
   */
  static class ObserverUnknownState extends ObserverState {
    private ObserverState previousState;

    ObserverUnknownState(ObserverState pPreviousState) {
      super();
      previousState = pPreviousState;
    }

    ObserverState getPreviousState() {
      return previousState;
    }
    @Override
    ObserverInternalState getInternalState() {
      return previousState.getInternalState();
    }
    @Override
    Map<String, ObserverVariable> getVars() {
      return previousState.getVars();
    }
    @Override
    public boolean isError() {
      return false;
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

    @Override
    protected ObserverAutomatonCPA getAutomatonCPA() {
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
  public String getCPAName() {
    return ObserverState.ObserverAnalysisNamePrefix + this.getAutomatonCPA().getAutomaton().getName();
  }

  ObserverInternalState getInternalState() {
    return this.internalState;
  }

  Map<String, ObserverVariable> getVars() {
   return vars;
  }
}
