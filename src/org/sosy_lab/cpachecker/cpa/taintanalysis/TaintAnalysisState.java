// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class TaintAnalysisState
    implements LatticeAbstractState<TaintAnalysisState>,
        Targetable,
        Serializable,
        Graphable,
        AbstractQueryableState {

  @Serial private static final long serialVersionUID = -7715698130795640052L;

  private boolean violatesProperty = false;
  private boolean isPathStart = false;

  private final Set<CExpression> taintedVariables = new HashSet<>();
  private final Set<CExpression> untaintedVariables = new HashSet<>();
  private static final String PROPERTY_TAINTED = "informationFlowViolation";
  private final Map<CExpression, List<CExpression>> evaluatedValues = new HashMap<>();

  private final List<TaintAnalysisState> predecessors = new ArrayList<>();
  private final List<TaintAnalysisState> successors = new ArrayList<>();
  private final List<TaintAnalysisState> siblingStates = new ArrayList<>();
  private final Set<TaintAnalysisState> nonTrivialPathStartStates = new HashSet<>();

  private static final List<TaintAnalysisState> targetStates = new ArrayList<>();

  public TaintAnalysisState(
      Set<CExpression> pTaintedVariables,
      Set<CExpression> pUntaintedVariables,
      Map<CExpression, List<CExpression>> pEvaluatedValues,
      Set<TaintAnalysisState> pPredecessors) {
    this.taintedVariables.addAll(pTaintedVariables);
    this.untaintedVariables.addAll(pUntaintedVariables);
    this.evaluatedValues.putAll(pEvaluatedValues);
    this.predecessors.addAll(pPredecessors);

    for (TaintAnalysisState predecessor : pPredecessors) {

      // Don't set this for joined states
      if (!this.isSuccessorOfJoinedState() && !predecessor.isSuccessorOfJoinedState()) {

        if (!predecessor.successors.isEmpty()) {
          this.isPathStart = true;
          this.nonTrivialPathStartStates.add(this);

          this.siblingStates.addAll(predecessor.successors);
          for (TaintAnalysisState sibling : this.siblingStates) {
            sibling.isPathStart = true;
            sibling.nonTrivialPathStartStates.add(sibling);
            sibling.siblingStates.add(this);
          }
        }

        if (!predecessor.siblingStates.isEmpty()) {
          // the predecessor is a non-trivial path-start-state.
          this.nonTrivialPathStartStates.add(predecessor);
        }
      }

      predecessor.successors.add(this);

      for (TaintAnalysisState pathStartState : predecessor.nonTrivialPathStartStates) {

        if (!pathStartState.isContainedIn(this.nonTrivialPathStartStates.stream().toList())) {
          this.nonTrivialPathStartStates.add(pathStartState);
        }
      }
    }
  }

  public boolean isSuccessorOfJoinedState() {
    return this.predecessors.size() > 1;
  }

  public boolean isPathStart() {
    return isPathStart;
  }

  public Set<TaintAnalysisState> getNonTrivialPathStartStates() {
    return nonTrivialPathStartStates;
  }

  public List<TaintAnalysisState> getPredecessors() {
    return predecessors;
  }

  public List<TaintAnalysisState> getSuccessors() {
    return successors;
  }

  public Map<CExpression, List<CExpression>> getEvaluatedValues() {
    return evaluatedValues;
  }

  public Set<CExpression> getTaintedVariables() {
    return taintedVariables;
  }

  public Set<CExpression> getUntaintedVariables() {
    return untaintedVariables;
  }

  /**
   * Determines whether the current TaintAnalysisState is less than or equal to another
   * TaintAnalysisState in the lattice by checking if all tainted variables in the current state are
   * also contained in the tainted variables of the other state.
   *
   * @param other the TaintAnalysisState to compare against
   * @return {@code true} if all tainted variables of the current state are contained in the other
   *     state's tainted variables, otherwise {@code false}
   */
  @Override
  public boolean isLessOrEqual(TaintAnalysisState other) {

    boolean allPredecessorsViolateProperty =
        this.predecessors.stream().allMatch(p -> p.violatesProperty)
            && other.predecessors.stream().allMatch(p -> p.violatesProperty);

    if (this.precedes(other)) {
      if (allPredecessorsViolateProperty) {
        other.setViolatesProperty();
      }

      return true;
    }

    if (!other.getTaintedVariables().containsAll(this.taintedVariables)) {
      return false;
    }

    Collection<TaintAnalysisState> thisSplitStates =
        TaintAnalysisUtils.getStatesWithSingeValueMapping(this);
    Collection<TaintAnalysisState> otherSplitStates =
        TaintAnalysisUtils.getStatesWithSingeValueMapping(other);

    if (!otherSplitStates.containsAll(thisSplitStates)) {
      return false;
    }

    if (allPredecessorsViolateProperty) {
      other.setViolatesProperty();
    }

    return true;
  }

  private boolean precedes(TaintAnalysisState pOther) {

    boolean otherContainsAllThisPathStarts =
        this.nonTrivialPathStartStates.stream()
            .allMatch(s -> s.isContainedIn(pOther.nonTrivialPathStartStates.stream().toList()));

    if (!otherContainsAllThisPathStarts) {
      return false;
    }

    if (this.nonTrivialPathStartStates.size() < pOther.nonTrivialPathStartStates.size()) {
      return true;
    }

    boolean otherContainsThisAsPredecessor = this.isContainedIn(pOther.predecessors);
    if (otherContainsThisAsPredecessor) {
      return true;
    } else {
      for (TaintAnalysisState predecessor : pOther.predecessors) {
        if (this.precedes(predecessor)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(taintedVariables, untaintedVariables, evaluatedValues);
  }

  /**
   * Compares the specified object with this TaintAnalysisState for equality. Returns {@code true}
   * if the specified object is also a TaintAnalysisState, and both have equivalent maps of tainted
   * variables.
   *
   * @param obj the object to be compared for equality with this state
   * @return {@code true} if the specified object is equal to this state; {@code false} otherwise
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof TaintAnalysisState other
        && Objects.equals(taintedVariables, other.taintedVariables)
        && Objects.equals(untaintedVariables, other.untaintedVariables)
        && Objects.equals(evaluatedValues, other.evaluatedValues);
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    return "{tainted: " + this.taintedVariables + ", untainted: " + this.untaintedVariables + "}";
  }

  @Override
  public TaintAnalysisState join(TaintAnalysisState pOther)
      throws CPAException, InterruptedException {
    if (this.isLessOrEqual(pOther)) {
      return pOther;
    } else if (pOther.isLessOrEqual(this)) {
      return this;
    }

    // Join the taint status
    Set<CExpression> joinedTaintedVars = new HashSet<>(this.taintedVariables);
    joinedTaintedVars.addAll(pOther.getTaintedVariables());

    Set<CExpression> joinedUntaintedVars = new HashSet<>();

    for (CExpression untaintedVar : this.untaintedVariables) {
      if (!joinedTaintedVars.contains(untaintedVar)) {
        joinedUntaintedVars.add(untaintedVar);
      }
    }

    for (CExpression untaintedVar : pOther.getUntaintedVariables()) {
      if (!joinedTaintedVars.contains(untaintedVar)) {
        joinedUntaintedVars.add(untaintedVar);
      }
    }

    Set<CExpression> allVars = new HashSet<>(joinedTaintedVars);
    allVars.addAll(joinedUntaintedVars);

    // join the variable to evaluated values mapping:
    Map<CExpression, List<CExpression>> joinEvaluatedValues = new HashMap<>();

    if (this.evaluatedValues == pOther.evaluatedValues) {
      joinEvaluatedValues = this.evaluatedValues;

    } else {
      int maxNumberOfMappedValuesInThisEvaluatedValues =
          this.evaluatedValues.values().stream().mapToInt(List::size).max().orElse(0);

      int maxNumberOfMappedValuesInOtherEvaluatedValues =
          pOther.evaluatedValues.values().stream().mapToInt(List::size).max().orElse(0);

      boolean thisEvaluatedVarsHasSimpleMapping = maxNumberOfMappedValuesInThisEvaluatedValues <= 1;
      boolean otherEvaluatedVarsHasSimpleMapping =
          maxNumberOfMappedValuesInOtherEvaluatedValues <= 1;

      boolean eachVariableIsMappedToTheSameValue =
          isEachVariableMappedToTheSameValue(allVars, this.evaluatedValues, pOther.evaluatedValues);

      boolean performFullJoinOfEvaluatedValues =
          thisEvaluatedVarsHasSimpleMapping
              && otherEvaluatedVarsHasSimpleMapping
              && eachVariableIsMappedToTheSameValue;

      if (performFullJoinOfEvaluatedValues) {

        for (CExpression var : allVars) {
          if (var != null) {
            if (this.evaluatedValues.containsKey(var) && pOther.evaluatedValues.containsKey(var)) {
              joinEvaluatedValues.put(var, this.evaluatedValues.get(var));
            } else if (this.evaluatedValues.containsKey(var)
                && !pOther.evaluatedValues.containsKey(var)) {
              joinEvaluatedValues.put(var, this.evaluatedValues.get(var));
            } else if (pOther.evaluatedValues.containsKey(var)
                && !this.evaluatedValues.containsKey(var)) {
              joinEvaluatedValues.put(var, pOther.evaluatedValues.get(var));
            } else {
              throw new IllegalStateException(
                  "At this point the variable '" + var + "' must be contained in at least one map");
            }
          }
        }

        for (List<CExpression> values : joinEvaluatedValues.values()) {
          if (values.size() > 1) {
            throw new AssertionError("At this point the values should be mapped to a single value");
          }
        }

      } else {
        int numberOfStatesToMerge =
            maxNumberOfMappedValuesInThisEvaluatedValues
                + maxNumberOfMappedValuesInOtherEvaluatedValues;

        for (CExpression var : allVars) {

          List<CExpression> thisValues =
              this.evaluatedValues.getOrDefault(
                  var,
                  new ArrayList<>(
                      Collections.nCopies(maxNumberOfMappedValuesInThisEvaluatedValues, null)));

          List<CExpression> otherValues =
              pOther.evaluatedValues.getOrDefault(
                  var,
                  new ArrayList<>(
                      Collections.nCopies(maxNumberOfMappedValuesInOtherEvaluatedValues, null)));

          List<CExpression> mergedValues = new ArrayList<>(numberOfStatesToMerge);
          mergedValues.addAll(thisValues);
          mergedValues.addAll(otherValues);

          joinEvaluatedValues.put(var, mergedValues);
        }
      }
    }

    Set<TaintAnalysisState> joinedPredecessors = new HashSet<>();
    joinedPredecessors.add(this);
    joinedPredecessors.add(pOther);

    return new TaintAnalysisState(
        joinedTaintedVars, joinedUntaintedVars, joinEvaluatedValues, joinedPredecessors);
  }

  private boolean isEachVariableMappedToTheSameValue(
      Set<CExpression> allVars,
      Map<CExpression, List<CExpression>> pThisEvaluatedValues,
      Map<CExpression, List<CExpression>> pOtherEvaluatedValues) {

    for (CExpression var : allVars) {
      if (pThisEvaluatedValues.containsKey(var) && pOtherEvaluatedValues.containsKey(var)) {
        if (!pThisEvaluatedValues.get(var).equals(pOtherEvaluatedValues.get(var))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return this.isTarget();
  }

  @Override
  public boolean isTarget() {
    return this.violatesProperty;
  }

  public TaintAnalysisState forceJoin() {

    List<TaintAnalysisState> statesToJoin =
        new ArrayList<>(
            targetStates.stream()
                .filter(
                    s ->
                        s.nonTrivialPathStartStates.size() <= this.nonTrivialPathStartStates.size())
                .toList());

    statesToJoin.add(this);

    if (statesToJoin.size() > 1) {

      return joinAllStates(statesToJoin);
    }

    return this;
  }

  private TaintAnalysisState joinAllStates(List<TaintAnalysisState> pStatesToJoin) {

    if (pStatesToJoin.size() <= 1) {
      throw new AssertionError("At this point there should be at least two merge points");
    }

    try {
      Deque<TaintAnalysisState> dequeStates = new ArrayDeque<>(pStatesToJoin);

      TaintAnalysisState joinedState = dequeStates.pollFirst();

      while (!dequeStates.isEmpty()) {
        assert joinedState != null;
        joinedState = joinedState.join(dequeStates.pollFirst());
      }

      return joinedState;
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException("Failed to join states: " + e.getMessage(), e);
    }
  }

  @Override
  public Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    if (isTarget()) {
      Set<TargetInformation> resSet = new HashSet<>();
      resSet.add(SimpleTargetInformation.create("Wrong assertion"));
      return resSet;
    } else {
      return new HashSet<>();
    }
  }

  @SuppressWarnings("unused")
  public void setViolatesProperty() {
    violatesProperty = true;
    if (this.isContainedIn(targetStates)) {
      targetStates.add(this);
    }
  }

  /**
   * Checks if the current TaintAnalysisState instance is contained in the given list of states by
   * comparing their identity hash codes. We use this check instead of the equals method when we
   * want to use intentional equality (object comparison instead of attributes comparison).
   *
   * @param pStates the list of TaintAnalysisState objects to search within
   * @return {@code true} if the current instance is contained in the given list, {@code false}
   *     otherwise
   */
  public boolean isContainedIn(List<TaintAnalysisState> pStates) {
    return pStates.stream().anyMatch(s -> s == this);
  }

  @Override
  public String getCPAName() {
    return "TaintAnalysisCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_TAINTED)) {
      return isTarget();
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }
}
