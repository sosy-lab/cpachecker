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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
  private final Map<CIdExpression, CExpression> taintedVariables;
  private final Map<CIdExpression, CExpression> untaintedVariables;
  private static final String PROPERTY_TAINTED = "informationFlowViolation";
  private Set<TaintAnalysisState> predecessors = new HashSet<>();
  private final Set<TaintAnalysisState> successors = new HashSet<>();
  private TaintAnalysisState siblingState;

  public TaintAnalysisState(Set<CIdExpression> pElements) {
    this.taintedVariables = new HashMap<>();
    this.untaintedVariables = new HashMap<>();
    for (CIdExpression expr : pElements) {
      this.taintedVariables.put(expr, null);
    }
  }

  public TaintAnalysisState(
      Map<CIdExpression, CExpression> pTaintedVariables,
      Map<CIdExpression, CExpression> pUntaintedVariables,
      Set<TaintAnalysisState> pPredecessors) {
    this.taintedVariables = new HashMap<>(pTaintedVariables);
    this.untaintedVariables = new HashMap<>(pUntaintedVariables);
    this.predecessors = pPredecessors;
    for (TaintAnalysisState predecessor : pPredecessors) {
      predecessor.addSuccessor(this);
    }
  }

  @Nullable
  public TaintAnalysisState getSiblingState() {
    return siblingState;
  }

  public Set<TaintAnalysisState> getPredecessors() {
    return predecessors;
  }

  public void setPredecessors(Set<TaintAnalysisState> pPredecessors) {
    predecessors = pPredecessors;
  }

  public Set<TaintAnalysisState> getSuccessors() {
    return successors;
  }

  public void addSuccessor(TaintAnalysisState pSuccessor) {
    successors.add(pSuccessor);
  }

  public Map<CIdExpression, CExpression> getTaintedVariables() {
    return taintedVariables;
  }

  public Map<CIdExpression, CExpression> getUntaintedVariables() {
    return untaintedVariables;
  }

  public Set<CIdExpression> getTaintedVariablesAsSet() {
    return taintedVariables.keySet();
  }

  @Override
  public boolean isLessOrEqual(TaintAnalysisState other) {
    // Verify that all tainted variables in "this" exist in "other" with the same values
    boolean otherContainsThisTainted =
        this.taintedVariables.entrySet().stream()
            .allMatch(
                entry ->
                    other.getTaintedVariables().containsKey(entry.getKey())
                        && Objects.equals(
                            other.getTaintedVariables().get(entry.getKey()), entry.getValue()));

    // Verify that all untainted variables in "this" exist in "other" with the same values
    boolean otherContainsThisUntainted =
        this.untaintedVariables.entrySet().stream()
            .allMatch(
                entry ->
                    other.getUntaintedVariables().containsKey(entry.getKey())
                        && Objects.equals(
                            other.getUntaintedVariables().get(entry.getKey()), entry.getValue()));

    return otherContainsThisTainted && otherContainsThisUntainted
    ;
  }

  @Override
  public int hashCode() {
    return Objects.hash(taintedVariables);
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
        && Objects.equals(untaintedVariables, other.untaintedVariables);
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    return "{tainted: "
        + this.taintedVariables.keySet()
        + ", untainted: "
        + this.untaintedVariables.keySet()
        + "}";
  }

  @Override
  public TaintAnalysisState join(TaintAnalysisState pOther)
      throws CPAException, InterruptedException {
    if (this.isLessOrEqual(pOther)) {
      return pOther;
    } else if (pOther.isLessOrEqual(this)) {
      return this;
    }

    Map<CIdExpression, CExpression> joinedTaintedVars = new HashMap<>(this.taintedVariables);
    Map<CIdExpression, CExpression> joinedUntaintedVars = new HashMap<>(this.untaintedVariables);

    // Add all tainted variables from the other state
    joinedTaintedVars.putAll(pOther.getTaintedVariables());

    // For untainted variables, only keep those that are untainted in both states
    joinedUntaintedVars.keySet().retainAll(pOther.getUntaintedVariables().keySet());

    Set<TaintAnalysisState> joinPredecessors = new HashSet<>(this.getPredecessors());
    joinPredecessors.addAll(pOther.getPredecessors());

    TaintAnalysisState joinedState =
        new TaintAnalysisState(
            joinedTaintedVars,
            joinedUntaintedVars,
            joinPredecessors);

    this.siblingState = pOther;
    pOther.siblingState = this;

    joinedState.getSuccessors().addAll(this.successors);
    joinedState.getSuccessors().addAll(pOther.getSuccessors());

    return joinedState;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return this.isTarget();
  }

  @Override
  public boolean isTarget() {
    return this.violatesProperty;
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
  }

  public void setViolatesProperty(boolean pViolatesProperty) {
    violatesProperty = pViolatesProperty;
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
