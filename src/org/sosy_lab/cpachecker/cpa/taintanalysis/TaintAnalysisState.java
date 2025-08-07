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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
  private final Set<CIdExpression> taintedVariables = new HashSet<>();
  private final Set<CIdExpression> untaintedVariables = new HashSet<>();
  private static final String PROPERTY_TAINTED = "informationFlowViolation";
  private final Map<CIdExpression, ArrayList<CExpression>> evaluatedValues = new HashMap<>();

  public TaintAnalysisState(Set<CIdExpression> pElements) {
    this.taintedVariables.addAll(pElements);
  }

  public TaintAnalysisState(
      Set<CIdExpression> pTaintedVariables,
      Set<CIdExpression> pUntaintedVariables,
      Map<CIdExpression, ArrayList<CExpression>> pEvaluatedValues) {
    this.taintedVariables.addAll(pTaintedVariables);
    this.untaintedVariables.addAll(pUntaintedVariables);
    this.evaluatedValues.putAll(pEvaluatedValues);
  }

  public Map<CIdExpression, ArrayList<CExpression>> getEvaluatedValues() {
    return evaluatedValues;
  }

  public Set<CIdExpression> getTaintedVariables() {
    return taintedVariables;
  }

  public Set<CIdExpression> getUntaintedVariables() {
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
    return other.getTaintedVariables().containsAll(this.taintedVariables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taintedVariables, untaintedVariables);
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
    Set<CIdExpression> joinedTaintedVars = new HashSet<>(this.taintedVariables);
    joinedTaintedVars.addAll(pOther.getTaintedVariables());

    Set<CIdExpression> joinedUntaintedVars = new HashSet<>();

    for (CIdExpression untaintedVar : this.untaintedVariables) {
      if (!joinedTaintedVars.contains(untaintedVar)) {
        joinedUntaintedVars.add(untaintedVar);
      }
    }

    for (CIdExpression untaintedVar : pOther.getUntaintedVariables()) {
      if (!joinedTaintedVars.contains(untaintedVar)) {
        joinedUntaintedVars.add(untaintedVar);
      }
    }

    Set<CIdExpression> allVars = new HashSet<>(joinedTaintedVars);
    allVars.addAll(joinedUntaintedVars);

    // join the variable to evaluated values mapping:
    Map<CIdExpression, ArrayList<CExpression>> joinEvaluatedValues = new HashMap<>();

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

        for (CIdExpression var : allVars) {
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
                "At this point the variable must be contained in at least one map");
          }
        }

        for (ArrayList<CExpression> values : joinEvaluatedValues.values()) {
          if (values.size() > 1) {
            throw new AssertionError("At this point the values should be mapped to a single value");
          }
        }

      } else {
        int numberOfStatesToMerge =
            maxNumberOfMappedValuesInThisEvaluatedValues
                + maxNumberOfMappedValuesInOtherEvaluatedValues;

        for (CIdExpression var : allVars) {

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

          ArrayList<CExpression> mergedValues = new ArrayList<>(numberOfStatesToMerge);
          mergedValues.addAll(thisValues);
          mergedValues.addAll(otherValues);

          joinEvaluatedValues.put(var, mergedValues);
        }
      }
    }

    TaintAnalysisState joinedState =
        new TaintAnalysisState(joinedTaintedVars, joinedUntaintedVars, joinEvaluatedValues);

    return joinedState;
  }

  private boolean isEachVariableMappedToTheSameValue(
      Set<CIdExpression> allVars,
      Map<CIdExpression, ArrayList<CExpression>> pThisEvaluatedValues,
      Map<CIdExpression, ArrayList<CExpression>> pOtherEvaluatedValues) {

    for (CIdExpression var : allVars) {
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
