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

import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * This class combines a AutomatonInternal State with a variable Configuration.
 * Instances of this class are passed to the CPAchecker as AbstractState.
 */
public class AutomatonState
    implements AbstractQueryableState, Targetable, Serializable, AbstractStateWithAssumptions, Graphable {

  private static final long serialVersionUID = -4665039439114057346L;
  private static final String AutomatonAnalysisNamePrefix = "AutomatonAnalysis_";

  static final String INTERNAL_STATE_IS_TARGET_PROPERTY = "internalStateIsTarget";

  static class TOP extends AutomatonState {

    private static final long serialVersionUID = -7848577870312049023L;

    public TOP(ControlAutomatonCPA pAutomatonCPA) {
      super(Collections.<String, AutomatonVariable> emptyMap(),
          AutomatonInternalState.TOP,
          pAutomatonCPA,
          ImmutableList.<Pair<AStatement, Boolean>> of(),
          0, 0, null);
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

  static class INACTIVE extends AutomatonState {

    private static final long serialVersionUID = -7848577870312049023L;

    public INACTIVE(ControlAutomatonCPA pAutomatonCPA) {
      super(Collections.<String, AutomatonVariable> emptyMap(),
          AutomatonInternalState.INACTIVE,
          pAutomatonCPA,
          ImmutableList.<Pair<AStatement, Boolean>> of(),
          0, 0, null);
    }

    @Override
    public boolean checkProperty(String pProperty) throws InvalidQueryException {
      return pProperty.toLowerCase().equals("state == inactive");
    }

    @Override
    public String toString() {
      return "AutomatonState.INACTIVE";
    }
  }

  static class BOTTOM extends AutomatonState {

    private static final long serialVersionUID = -401794748742705212L;

    public BOTTOM(ControlAutomatonCPA pAutomatonCPA) {
      super(Collections.<String, AutomatonVariable> emptyMap(),
          AutomatonInternalState.BOTTOM,
          pAutomatonCPA, ImmutableList.<Pair<AStatement, Boolean>> of(),
          0, 0, null);
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

  private transient ControlAutomatonCPA automatonCPA;

  private final Map<String, AutomatonVariable> vars;
  private transient AutomatonInternalState internalState;
  private final ImmutableList<Pair<AStatement, Boolean>> assumptions;

  private final Map<? extends Property, ResultValue<?>> violatedPropertyInstance;

  private int matches = 0;
  private int failedMatches = 0;

  static AutomatonState automatonStateFactory(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA,
      ImmutableList<Pair<AStatement, Boolean>> pInstantiatedAssumes,
      int successfulMatches, int failedMatches,
      Map<? extends Property, ResultValue<?>> pViolatedProperties) {

    if (pInternalState == AutomatonInternalState.BOTTOM) {
      return pAutomatonCPA.getBottomState();
    } else {
      return new AutomatonState(pVars, pInternalState, pAutomatonCPA,
          pInstantiatedAssumes, successfulMatches, failedMatches,
          pViolatedProperties);
    }
  }

  static AutomatonState automatonStateFactory(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState, ControlAutomatonCPA pAutomatonCPA,
      int successfulMatches, int failedMatches,
      Map<? extends Property, ResultValue<?>> pViolatedProperties) {

    return automatonStateFactory(pVars, pInternalState, pAutomatonCPA,
        ImmutableList.<Pair<AStatement, Boolean>> of(),
        successfulMatches, failedMatches, pViolatedProperties);
  }

  private AutomatonState(Map<String, AutomatonVariable> pVars,
      AutomatonInternalState pInternalState,
      ControlAutomatonCPA pAutomatonCPA,
      ImmutableList<Pair<AStatement, Boolean>> pInstantiatedAssumes,
      int successfulMatches,
      int failedMatches,
      Map<? extends Property, ResultValue<?>> pViolatedProperties) {

    this.vars = checkNotNull(pVars);
    this.internalState = checkNotNull(pInternalState);
    this.automatonCPA = checkNotNull(pAutomatonCPA);
    this.matches = successfulMatches;
    this.failedMatches = failedMatches;
    this.assumptions = pInstantiatedAssumes;

    if (isTarget()) {
      checkArgument(pViolatedProperties.size() > 0);
      violatedPropertyInstance = pViolatedProperties;
    } else {
      violatedPropertyInstance = null;
    }
  }

  @Override
  public boolean isTarget() {
    return this.automatonCPA.isTreatingErrorsAsTargets() && internalState.isTarget();
  }

  public ImmutableMap<? extends Property, ResultValue<?>> getViolatedPropertyInstances() {
    return ImmutableMap.copyOf(violatedPropertyInstance);
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    return ImmutableSet.<Property>copyOf(violatedPropertyInstance.keySet());
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) { return true; }
    if (pObj == null) { return false; }
    if (!pObj.getClass().equals(this.getClass())) { return false; }
    AutomatonState otherState = (AutomatonState) pObj;

    return this.internalState.equals(otherState.internalState)
        && this.vars.equals(otherState.vars)
        // the same state of the internal automata might be instantiated with different assumptions.
        && this.assumptions.equals(otherState.assumptions);
  }

  @Override
  public int hashCode() {
    // Important: we cannot use vars.hashCode(), because the hash code of a map
    // depends on the hash code of its values, and those may change.
    return internalState.hashCode();
  }

  public List<CStatementEdge> getAsStatementEdges(String cFunctionName) {
    if (assumptions.isEmpty()) { return ImmutableList.of(); }

    List<CStatementEdge> result = new ArrayList<>(assumptions.size());
    for (Pair<AStatement, Boolean> entry : assumptions) {
      final AStatement statement = entry.getFirst();

      if (statement instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {


          result.add(new CStatementEdge(assignment.toASTString(), assignment, assignment.getFileLocation(),
              new CFANode(cFunctionName), new CFANode(cFunctionName)));
        } else if (assignment.getRightHandSide() instanceof CFunctionCall) {
          //TODO FunctionCalls, ExpressionStatements etc
        }
      }
    }
    return result;
  }

  /**
   * Translate the assumptions of this state into a list of assume
   * edges that can be evaluated by, for example, the strengthening operator of a CPA.
   */
  @Override
  public List<AssumeEdge> getAsAssumeEdges(String cFunctionName) {
    if (assumptions.isEmpty()) { return ImmutableList.of(); }

    List<AssumeEdge> result = new ArrayList<>(assumptions.size());
    CBinaryExpressionBuilder expressionBuilder =
        new CBinaryExpressionBuilder(automatonCPA.getMachineModel(), automatonCPA.getLogManager());

    for (Pair<AStatement, Boolean> entry : assumptions) {
      final AStatement statement = entry.getFirst();
      final boolean truthStatement = entry.getSecond();

      if (statement instanceof CAssignment) {
        // Assignments
        //  (but we would prefer boolean expressions as assumes!!)

        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {

          CExpression expression = (CExpression) assignment.getRightHandSide();
          CBinaryExpression assumeExp =
              expressionBuilder.buildBinaryExpressionUnchecked(
                  assignment.getLeftHandSide(),
                  expression,
                  CBinaryExpression.BinaryOperator.EQUALS);

          result.add(new CAssumeEdge(
              assignment.toASTString(),
              assignment.getFileLocation(),
              new CFANode(cFunctionName),
              new CFANode(cFunctionName),
              assumeExp, truthStatement));

        } else {

          throw new RuntimeException(String.format("Support for %s as a right-hand side not yet implemented!",
              assignment.getRightHandSide().getClass().getSimpleName()));
        }

      } else if (statement instanceof CExpressionStatement) {
        // Assumptions
        //  (the prefered way of providing assumptions are boolean expressions)

        //        final CExpressionStatement exprStmt = (CExpressionStatement) statement;
        //        if (exprStmt.getExpression().getExpressionType() instanceof CSimpleType
        //            && ((CSimpleType) (exprStmt.getExpression().getExpressionType())).getType().isIntegerType()) {
        // <-- why would this condition be necessary??

        result.add(new CAssumeEdge(
            statement.toASTString(),
            statement.getFileLocation(),
            new CFANode(cFunctionName),
            new CFANode(cFunctionName),
            ((CExpressionStatement) statement).getExpression(),
            truthStatement));

      }
    }

    return result;
  }

  /**
   * returns the name of the automaton, to whom this state belongs to (the name is specified in the automaton file)
   * forwards to <code>automatonCPA.getAutomaton().getName()</code>.
   * @return name of automaton
   */
  public String getOwningAutomatonName() {
    return automatonCPA.getAutomaton().getName();
  }

  public Automaton getOwningAutomaton() {
    return automatonCPA.getAutomaton();
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(automatonCPA == null ? "" : (automatonCPA.getAutomaton().getName() + ": "));
    result.append(internalState.getName());
    result.append(isTarget() ? " TARGET " : "");
    result.append(" ");
    result.append(Joiner.on(' ').withKeyValueSeparator("=").join(vars));

    return result.toString();
  }

  @Override
  public String toDOTLabel() {
    return toString();
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
      super(pPreviousState.getVars(), pPreviousState.getInternalState(), pPreviousState.automatonCPA,
          pPreviousState.getAssumptions(), -1, -1, null);
      previousState = pPreviousState;
    }

    AutomatonState getPreviousState() {
      return previousState;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) { return true; }
      if (pObj == null) { return false; }
      if (!pObj.getClass().equals(this.getClass())) { return false; }
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
    if (pProperty.equalsIgnoreCase(INTERNAL_STATE_IS_TARGET_PROPERTY)) { return getInternalState().isTarget(); }
    String[] parts = pProperty.split("==");
    if (parts.length != 2) {
      throw new InvalidQueryException(
          "The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
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
            throw new InvalidQueryException(
                "The Query \"" + pProperty + "\" is invalid. Could not parse the int \"" + right + "\".");
          }
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty
              + "\" is invalid. Only accepting \"State == something\" and \"varname = something\" queries so far.");
        }
      }
    }
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // allows to set values of Automaton variables like "x:=6"
    String[] parts = pModification.split(":=");
    if (parts.length != 2) {
      throw new InvalidQueryException(
          "The Query \"" + pModification + "\" is invalid. Could not split the string correctly.");
    } else {
      String left = parts[0].trim();
      String right = parts[1].trim();
      AutomatonVariable var = this.vars.get(left);
      if (var != null) {
        try {
          int val = Integer.parseInt(right);
          var.setValue(val);
        } catch (NumberFormatException e) {
          throw new InvalidQueryException(
              "The Query \"" + pModification + "\" is invalid. Could not parse the int \"" + right + "\".");
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
  public ImmutableList<Pair<AStatement, Boolean>> getAssumptions() {
    return assumptions;
  }

  public AutomatonInternalState getInternalState() {
    return internalState;
  }

  public String getInternalStateName() {
    return internalState.getName();
  }

  Map<String, AutomatonVariable> getVars() {
    return vars;
  }

  ControlAutomatonCPA getAutomatonCPA() {
    return automatonCPA;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(internalState.getStateId());
    out.writeObject(automatonCPA.getAutomaton().getName());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int stateId = in.readInt();
    internalState = GlobalInfo.getInstance().getAutomatonInfo().getStateById(stateId);
    automatonCPA = GlobalInfo.getInstance().getAutomatonInfo().getCPAForAutomaton((String) in.readObject());
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
