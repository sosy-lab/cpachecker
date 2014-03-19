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

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * This class combines a AutomatonInternal State with a variable Configuration.
 * Instances of this class are passed to the CPAchecker as AbstractState.
 */
public class AutomatonState implements AbstractQueryableState, Targetable, Serializable, Partitionable, AbstractStateWithAssumptions {

  private static final long serialVersionUID = -4665039439114057346L;
  private static final String AutomatonAnalysisNamePrefix = "AutomatonAnalysis_";

  static class TOP extends AutomatonState {
    private static final long serialVersionUID = -7848577870312049023L;

    public TOP(ControlAutomatonCPA pAutomatonCPA) {
      super(Collections.<String, AutomatonVariable>emptyMap(),
            new AutomatonInternalState("_predefinedState_TOP", Collections.<AutomatonTransition>emptyList()),
            pAutomatonCPA, ImmutableList.<IAStatement>of(), 0, 0);
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

    public BOTTOM(ControlAutomatonCPA pAutomatonCPA) {
      super(Collections.<String, AutomatonVariable>emptyMap(),
            AutomatonInternalState.BOTTOM,
            pAutomatonCPA, ImmutableList.<IAStatement>of(), 0, 0);
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

  private transient final ControlAutomatonCPA automatonCPA;
  private final Map<String, AutomatonVariable> vars;
  private transient AutomatonInternalState internalState;
  private final ImmutableList<IAStatement> assumptions;
  private int matches = 0;
  private int failedMatches = 0;
  private Set<Integer> tokensSinceLastMatch = null;

  static AutomatonState automatonStateFactory(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA,
      ImmutableList<IAStatement> pAssumptions, int successfulMatches, int failedMatches) {

    if (pInternalState == AutomatonInternalState.BOTTOM) {
      return pAutomatonCPA.getBottomState();
    } else {
      return new AutomatonState(pVars, pInternalState, pAutomatonCPA, pAssumptions, successfulMatches, failedMatches);
    }
  }

  static AutomatonState automatonStateFactory(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA,
      int successfulMatches, int failedMatches) {
    return automatonStateFactory(pVars, pInternalState, pAutomatonCPA, ImmutableList.<IAStatement>of(), successfulMatches, failedMatches);
  }

  private AutomatonState(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      ControlAutomatonCPA pAutomatonCPA,
      ImmutableList<IAStatement> pAssumptions,
      int successfulMatches,
      int failedMatches) {

    this.vars = checkNotNull(pVars);
    this.internalState = checkNotNull(pInternalState);
    this.automatonCPA = checkNotNull(pAutomatonCPA);
    this.matches = successfulMatches;
    this.failedMatches = failedMatches;
    assumptions = pAssumptions;
  }

  @Override
  public boolean isTarget() {
    return internalState.isTarget();
  }

  @Override
  public ViolatedProperty getViolatedProperty() throws IllegalStateException {
    checkState(isTarget());
    return ViolatedProperty.OTHER;
  }

  @Override
  public Object getPartitionKey() {
    return internalState;
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

    return this.internalState.equals(otherState.internalState)
        && this.vars.equals(otherState.vars);
  }

  @Override
  public int hashCode() {
    // Important: we cannot use vars.hashCode(), because the hash code of a map
    // depends on the hash code of its values, and those may change.
    return internalState.hashCode();
  }

  @Override
  public List<AssumeEdge> getAsAssumeEdges(AIdExpression name_of_return_Var, String cFunctionName) {
    if (assumptions.isEmpty()) {
      return ImmutableList.of();
    }

    List<AssumeEdge> result = new ArrayList<>(assumptions.size());
    for(IAStatement statement : assumptions) {

      if(statement instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {

          CExpression expression = (CExpression) assignment.getRightHandSide();
          CBinaryExpression assumeExp =
              new CBinaryExpression(assignment.getFileLocation(), CNumericTypes.BOOL, CNumericTypes.INT, assignment.getLeftHandSide(),
                  expression, CBinaryExpression.BinaryOperator.EQUALS);

          result.add(new CAssumeEdge(assignment.toASTString(), assignment.getFileLocation(),
              new CFANode(cFunctionName), new CFANode(cFunctionName), assumeExp, true));
        } else if(assignment.getRightHandSide() instanceof CFunctionCall) {
          //TODO FunctionCalls, ExpressionStatements etc
        }
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return (automatonCPA!=null?automatonCPA.getAutomaton().getName() + ": ": "") + internalState.getName() + ' ' + Joiner.on(' ').withKeyValueSeparator("=").join(vars);
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
      super(pPreviousState.getVars(), pPreviousState.getInternalState(), pPreviousState.automatonCPA, pPreviousState.getAssumptions(), -1, -1);
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
      return "AutomatonUnknownState<" + previousState.toString() + ">";
    }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // e.g. "state == name-of-state" where name-of state can be top, bottom, error, or any state defined in the automaton definition.
    String[] parts = pProperty.split("==");
    if (parts.length != 2) {
      throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
    } else {
      String left = parts[0].trim();
      String right = parts[1].trim();
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
    String[] parts = pModification.split(":=");
    if (parts.length != 2) {
      throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not split the string correctly.");
    } else {
      String left = parts[0].trim();
      String right = parts[1].trim();
      AutomatonVariable var = this.vars.get(left);
      if (var != null) {
        try {
          int val = Integer.parseInt(right);
          var.setValue(val);
        } catch (NumberFormatException e) {
          throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not parse the int \"" + right + "\".");
        }
      } else {
        throw new InvalidQueryException("Could not modify the variable \"" + left + "\" (Variable not found)");
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

  @Override
  public ImmutableList<IAStatement> getAssumptions() {
    return assumptions;
  }

  AutomatonInternalState getInternalState() {
    return internalState;
  }

  public String getInternalStateName() {
    return internalState.getName();
  }

  Map<String, AutomatonVariable> getVars() {
   return vars;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(internalState.getStateId());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int stateId = in.readInt();
    internalState = GlobalInfo.getInstance().getAutomatonInfo().getStateById(stateId);
  }

  public int getMatches() {
    return matches;
  }

  public int getFailedMatches() {
    return failedMatches;
  }

  public Set<Integer> getTokensSinceLastMatch() {
    if (tokensSinceLastMatch == null) {
      return Collections.emptySet();
    } else {
      return tokensSinceLastMatch;
    }
  }

  public void addNoMatchTokens(Set<Integer> pTokens) {
    if (tokensSinceLastMatch == null) {
      tokensSinceLastMatch = Sets.newTreeSet();
    }
    tokensSinceLastMatch.addAll(pTokens);
  }

  public void setFailedMatches(int pFailedMatches) {
    failedMatches = pFailedMatches;
  }

  public void setMatches(int pMatches) {
    matches = pMatches;
  }
}
