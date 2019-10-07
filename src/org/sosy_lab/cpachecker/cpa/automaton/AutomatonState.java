/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

/**
 * This class combines a AutomatonInternal State with a variable Configuration.
 * Instances of this class are passed to the CPAchecker as AbstractState.
 */
public class AutomatonState implements AbstractQueryableState, Targetable, Serializable, AbstractStateWithAssumptions, Graphable {

  private static final long serialVersionUID = -4665039439114057346L;
  private static final String AutomatonAnalysisNamePrefix = "AutomatonAnalysis_";

  static final String INTERNAL_STATE_IS_TARGET_PROPERTY = "internalStateIsTarget";

  static class TOP extends AutomatonState {
    private static final long serialVersionUID = -7848577870312049023L;

    public TOP(Automaton pAutomaton, boolean pTreatErrorsAsTarget) {
      super(
          ImmutableMap.of(),
          new AutomatonInternalState("_predefinedState_TOP", ImmutableList.of()),
          pAutomaton,
          ImmutableList.of(),
          ExpressionTrees.getTrue(),
          0,
          0,
          null,
          pTreatErrorsAsTarget);
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
    private static final long serialVersionUID = -401794748742705212L;

    public BOTTOM(Automaton pAutomaton, boolean pTreatErrorsAsTarget) {
      super(
          ImmutableMap.of(),
          AutomatonInternalState.BOTTOM,
          pAutomaton,
          ImmutableList.of(),
          ExpressionTrees.getTrue(),
          0,
          0,
          null,
          pTreatErrorsAsTarget);
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

  private transient Automaton automaton;
  private final Map<String, AutomatonVariable> vars;
  private transient AutomatonInternalState internalState;
  private final ImmutableList<AExpression> assumptions;
  private transient final ExpressionTree<AExpression> candidateInvariants;
  private int matches = 0;
  private int failedMatches = 0;
  private transient final AutomatonSafetyProperty violatedPropertyDescription;
  private final boolean treatErrorAsTarget;

  static AutomatonState automatonStateFactory(
      Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      Automaton pAutomaton,
      ImmutableList<AExpression> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      int successfulMatches,
      int failedMatches,
      AutomatonSafetyProperty violatedPropertyDescription,
      boolean pTreatErrorAsTarget) {

    if (pInternalState == AutomatonInternalState.BOTTOM) {
      return new AutomatonState.BOTTOM(pAutomaton, pTreatErrorAsTarget);
    } else {
      return new AutomatonState(
          pVars,
          pInternalState,
          pAutomaton,
          pAssumptions,
          pCandidateInvariants,
          successfulMatches,
          failedMatches,
          violatedPropertyDescription,
          pTreatErrorAsTarget);
    }
  }

  static AutomatonState automatonStateFactory(
      Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      Automaton pAutomaton,
      int successfulMatches,
      int failedMatches,
      AutomatonSafetyProperty violatedPropertyDescription,
      boolean pTreatErrorAsTarget) {
    return automatonStateFactory(
        pVars,
        pInternalState,
        pAutomaton,
        ImmutableList.of(),
        ExpressionTrees.getTrue(),
        successfulMatches,
        failedMatches,
        violatedPropertyDescription,
        pTreatErrorAsTarget);
  }

  private AutomatonState(
      Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      Automaton pAutomaton,
      ImmutableList<AExpression> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      int successfulMatches,
      int failedMatches,
      AutomatonSafetyProperty pViolatedPropertyDescription,
      boolean pTreatErrorAsTarget) {

    this.vars = checkNotNull(pVars);
    this.internalState = checkNotNull(pInternalState);
    this.automaton = checkNotNull(pAutomaton);
    this.matches = successfulMatches;
    this.failedMatches = failedMatches;
    this.assumptions = pAssumptions;
    this.candidateInvariants = pCandidateInvariants;
    this.treatErrorAsTarget = pTreatErrorAsTarget;

    if (internalState.isTarget()) {
      checkNotNull(pViolatedPropertyDescription);
      violatedPropertyDescription = pViolatedPropertyDescription;
    } else {
      violatedPropertyDescription = null;
    }
  }

  @Override
  public boolean isTarget() {
    return treatErrorAsTarget && internalState.isTarget();
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    return ImmutableSet.<Property>of(violatedPropertyDescription);
  }

  Optional<AutomatonSafetyProperty> getOptionalViolatedPropertyDescription() {
    return Optional.ofNullable(violatedPropertyDescription);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj == null) {
      return false;
    }
    if (!pObj.getClass().equals(this.getClass())) {
      return false;
    }

    AutomatonState otherState = (AutomatonState) pObj;
    return Objects.equals(assumptions, otherState.assumptions)
        && Objects.equals(vars, otherState.vars)
        && Objects.equals(internalState, otherState.internalState);
  }

  @Override
  public int hashCode() {
    // Important: we cannot use vars.hashCode(), because the hash code of a map
    // depends on the hash code of its values, and those may change.
    return Objects.hash(assumptions, internalState);
  }


  @Override
  public ImmutableList<AExpression> getAssumptions() {
    return assumptions;
  }

  /**
   * returns the name of the automaton, to whom this state belongs to (the name is specified in the automaton file)
   * forwards to <code>automatonCPA.getAutomaton().getName()</code>.
   * @return name of automaton
   */
  public String getOwningAutomatonName() {
    return automaton.getName();
  }

  public Automaton getOwningAutomaton() {
    return automaton;
  }

  @Override
  public String toString() {
    return (automaton != null ? automaton.getName() + ": " : "")
        + internalState.getName()
        + ' '
        + Joiner.on(' ').withKeyValueSeparator("=").join(vars);
  }

  @Override
  public String toDOTLabel() {
    if (!internalState.getName().equals("Init")) {
      String prettyPrintAsmpts = "";
      if (!assumptions.isEmpty()) {
        prettyPrintAsmpts =
            "\nAssumptions: "
                + assumptions
                    .stream()
                    .map(AExpression::toASTString)
                    .collect(Collectors.joining("; "));
      }
      return (automaton != null ? automaton.getName() + ": " : "")
          + internalState.getName()
          + prettyPrintAsmpts;
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /**
   * The UnknownState represents one of the States following a normal State of the Automaton.
   * Which State is the correct following state could not be determined so far.
   * This Class is used if during a "getAbstractSuccessor" call the abstract successor could not be determined.
   * During the subsequent "strengthen" call enough information should be available to determine a normal AutomatonState as following State.
   */
  static class AutomatonUnknownState extends AutomatonState {
    private static final long serialVersionUID = -2010032222354565037L;
    private final AutomatonState previousState;

    AutomatonUnknownState(AutomatonState pPreviousState) {
      super(
          pPreviousState.getVars(),
          pPreviousState.getInternalState(),
          pPreviousState.automaton,
          ImmutableList.of(),
          ExpressionTrees.getTrue(),
          -1,
          -1,
          pPreviousState.violatedPropertyDescription,
          pPreviousState.isTreatingErrorsAsTarget());
      previousState = pPreviousState;
    }

    AutomatonState getPreviousState() {
      return previousState;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj == null) {
        return false;
      }
      if (!pObj.getClass().equals(this.getClass())) {
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
      return "AutomatonUnknownState<" + previousState + ">";
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    /*
     * Check properties of the state, which are either:
     * a) "internalStateIsTarget", to check if the internal state is a target
     *    state.
     * b) "state == name-of-state" where name-of-state is the name of the
     *    internal state, e.g. _predefinedState_ERROR, _predefinedState_BOTTOM,
     *    _predefinedState_BREAK.
     * c) "name-of-variable == int-value" where name-of-variable is the name of
     *    an automaton variable and int-value is an integer value.
     */
    if (pProperty.equalsIgnoreCase(INTERNAL_STATE_IS_TARGET_PROPERTY)) {
      return getInternalState().isTarget();
    }
    List<String> parts = Splitter.on("==").trimResults().splitToList(pProperty);
    if (parts.size() != 2) {
      throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
    } else {
      String left = parts.get(0);
      String right = parts.get(1);
      if (left.equalsIgnoreCase("state")) {
        return this.getInternalState().getName().equals(right);
      } else {
        AutomatonVariable var = vars.get(left);
        if (var != null) {
          // is a local variable
          try {
            int val = Integer.parseInt(right);
            return var.getValue() == val;
          } catch (NumberFormatException e) {
            throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not parse the int \"" + right + "\".");
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
    List<String> parts = Splitter.on(":=").trimResults().splitToList(pModification);
    if (parts.size() != 2) {
      throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not split the string correctly.");
    } else {
      String left = parts.get(0);
      String right = parts.get(1);
      AutomatonVariable var = this.vars.get(left);
      if (var != null) {
        if (var instanceof AutomatonIntVariable) {
          try {
            int val = Integer.parseInt(right);
            ((AutomatonIntVariable) var).setValue(val);
          } catch (NumberFormatException e) {
            throw new InvalidQueryException(
                "The Query \""
                    + pModification
                    + "\" is invalid. Could not parse the int \""
                    + right
                    + "\".");
          }
        } else {
          throw new InvalidQueryException(
              "Automaton variable '"
                  + var.getName()
                  + "' is not supported in query '"
                  + pModification
                  + "'");
        }
      } else {
        throw new InvalidQueryException("Could not modify the variable \"" + left + "\" (Variable not found)");
      }
    }
  }

  @Override
  public String getCPAName() {
    return AutomatonState.AutomatonAnalysisNamePrefix + automaton.getName();
  }

  boolean isTreatingErrorsAsTarget() {
    return treatErrorAsTarget;
  }

  public ExpressionTree<AExpression> getCandidateInvariants() {
    return candidateInvariants;
  }

  AutomatonInternalState getInternalState() {
    return internalState;
  }

  public String getInternalStateName() {
    return internalState.getName();
  }

  public Map<String, AutomatonVariable> getVars() {
   return vars;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(internalState.getStateId());
    out.writeObject(automaton.getName());
  }

  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int stateId = in.readInt();
    internalState = GlobalInfo.getInstance().getAutomatonInfo().getStateById(stateId);
    if(internalState == null) {
      if(stateId == AutomatonInternalState.ERROR.getStateId()) {
        internalState = AutomatonInternalState.ERROR;
      } else if(stateId == AutomatonInternalState.BREAK.getStateId()) {
        internalState = AutomatonInternalState.BREAK;
      } else if(stateId == AutomatonInternalState.BOTTOM.getStateId()) {
        internalState = AutomatonInternalState.BOTTOM;
      }
    }

    automaton =
        GlobalInfo.getInstance()
            .getAutomatonInfo()
            .getCPAForAutomaton((String) in.readObject())
            .getAutomaton();
  }

  public int getMatches() {
    return matches;
  }

  public int getFailedMatches() {
    return failedMatches;
  }

  public void setFailedMatches(int pFailedMatches) {
    failedMatches = pFailedMatches;
  }

  public void setMatches(int pMatches) {
    matches = pMatches;
  }
}
