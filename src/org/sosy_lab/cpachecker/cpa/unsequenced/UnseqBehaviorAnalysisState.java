// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class UnseqBehaviorAnalysisState
    implements LatticeAbstractState<UnseqBehaviorAnalysisState>, Graphable, AbstractQueryableState {

  // Property
  private static final String UNSEQUENCED = "has-unsequenced-execution";

  private final Map<String, Set<SideEffectInfo>> sideEffectsInFun; // total side effects
  private final Deque<String> calledFunctionStack;
  private final Set<ConflictPair> detectedConflicts;
  private final Map<String, CRightHandSide> tmpToOriginalExprMap;

  public UnseqBehaviorAnalysisState(
      Map<String, Set<SideEffectInfo>> pSideEffectsInFun,
      Deque<String> pCalledFunctionStack,
      Set<ConflictPair> pDetectedConflicts,
      Map<String, CRightHandSide> pTmpToOriginalExprMap) {
    sideEffectsInFun = pSideEffectsInFun;
    calledFunctionStack = pCalledFunctionStack;
    detectedConflicts = pDetectedConflicts;
    tmpToOriginalExprMap = pTmpToOriginalExprMap;
  }

  public UnseqBehaviorAnalysisState() {
    sideEffectsInFun = new HashMap<>();
    calledFunctionStack = new ArrayDeque<>();
    detectedConflicts = new HashSet<>();
    tmpToOriginalExprMap = new HashMap<>();
  }

  // === Side effect management ===
  public Map<String, Set<SideEffectInfo>> getSideEffectsInFun() {
    return sideEffectsInFun;
  }

  public void addSideEffectsToFunction(String functionName, Set<SideEffectInfo> newEffects) {
    sideEffectsInFun.computeIfAbsent(functionName, k -> new HashSet<>()).addAll(newEffects);
  }

  // === Conflict tracking ===
  public void addConflicts(Set<ConflictPair> conflicts) {
    detectedConflicts.addAll(conflicts);
  }

  public Set<ConflictPair> getDetectedConflicts() {
    return detectedConflicts;
  }

  // === Function call tracking ===
  public void pushCalledFunction(String functionName) {
    calledFunctionStack.push(functionName);
  }

  public void popCalledFunction() {
    if (!calledFunctionStack.isEmpty()) {
      calledFunctionStack.pop();
    }
  }

  public boolean isInsideFunctionCall() {
    return !calledFunctionStack.isEmpty();
  }

  @Nullable
  public String getCurrentCalledFunction() {
    return calledFunctionStack.peek();
  }

  public Deque<String> getCalledFunctionStack() {
    return calledFunctionStack;
  }

  // === TMP to expression mapping ===
  public Map<String, CRightHandSide> getTmpToOriginalExprMap() {
    return tmpToOriginalExprMap;
  }

  public void mapTmpToFunction(String tmpVar, CRightHandSide function) {
    tmpToOriginalExprMap.put(tmpVar, function);
  }

  public CRightHandSide getFunctionForTmp(String tmpVar) {
    return tmpToOriginalExprMap.get(tmpVar);
  }

  @Override
  public boolean equals(@Nullable Object pOther) {
    if (this == pOther) {
      return true;
    }
    // Intentionally using instanceof instead of getClass() to comply with ErrorProne
    if (!(pOther instanceof UnseqBehaviorAnalysisState other)) {
      return false;
    }
    return Objects.equals(calledFunctionStack, other.calledFunctionStack)
        && Objects.equals(sideEffectsInFun, other.sideEffectsInFun)
        && Objects.equals(detectedConflicts, other.detectedConflicts)
        && Objects.equals(tmpToOriginalExprMap, other.tmpToOriginalExprMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        sideEffectsInFun, calledFunctionStack, detectedConflicts, tmpToOriginalExprMap);
  }

  public String printConflict() {
    if (detectedConflicts.isEmpty()) {
      return "conflicts[]";
    }

    StringBuilder sb = new StringBuilder(detectedConflicts.size() + " conflicts[");

    boolean first = true;
    for (ConflictPair conflict : detectedConflicts) {
      if (!first) {
        sb.append(", ");
      } else {
        first = false;
      }
      sb.append(conflict);
    }

    sb.append("]");
    return sb.toString();
  }

  @Override
  public String toDOTLabel() {
    return printConflict();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public UnseqBehaviorAnalysisState join(UnseqBehaviorAnalysisState other)
      throws CPAException, InterruptedException {
    if (!Objects.equals(this.calledFunctionStack, other.calledFunctionStack)) {
      throw new CPAException("Cannot join states with different function call stacks.");
    }

    UnseqBehaviorAnalysisState newState = new UnseqBehaviorAnalysisState();

    for (Map.Entry<String, Set<SideEffectInfo>> entry : this.getSideEffectsInFun().entrySet()) {
      newState.getSideEffectsInFun().put(entry.getKey(), new HashSet<>(entry.getValue()));
    }
    for (Map.Entry<String, Set<SideEffectInfo>> entry : other.getSideEffectsInFun().entrySet()) {
      newState
          .getSideEffectsInFun()
          .merge(
              entry.getKey(),
              new HashSet<>(entry.getValue()),
              (oldSet, newSet) -> {
                oldSet.addAll(newSet);
                return oldSet;
              });
    }

    for (Map.Entry<String, CRightHandSide> entry : this.getTmpToOriginalExprMap().entrySet()) {
      newState.getTmpToOriginalExprMap().put(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, CRightHandSide> entry : other.getTmpToOriginalExprMap().entrySet()) {
      newState
          .getTmpToOriginalExprMap()
          .merge(
              entry.getKey(),
              entry.getValue(),
              (v1, v2) -> {
                if (!v1.equals(v2)) {
                  throw new IllegalStateException(
                      "Conflicting tmp mappings during join: " + v1 + " vs " + v2);
                }
                return v1;
              });
    }

    newState.getDetectedConflicts().addAll(this.getDetectedConflicts());
    newState.getDetectedConflicts().addAll(other.getDetectedConflicts());
    newState.calledFunctionStack.addAll(this.calledFunctionStack);

    return newState;
  }

  @Override
  public boolean isLessOrEqual(UnseqBehaviorAnalysisState reachedState)
      throws CPAException, InterruptedException {
    if (!Objects.equals(this.calledFunctionStack, reachedState.calledFunctionStack)) {
      return false;
    }

    // Compare side effects in functions
    for (Map.Entry<String, Set<SideEffectInfo>> entry : this.getSideEffectsInFun().entrySet()) {
      String functionName = entry.getKey();
      Set<SideEffectInfo> thisEffects = entry.getValue();
      Set<SideEffectInfo> reachedStateEffects =
          reachedState.getSideEffectsInFun().get(functionName);

      if (reachedStateEffects == null || !reachedStateEffects.containsAll(thisEffects)) {
        return false;
      }
    }

    // Compare TMP → Original Expression mappings
    if (!reachedState
        .getTmpToOriginalExprMap()
        .entrySet()
        .containsAll(this.getTmpToOriginalExprMap().entrySet())) {
      return false;
    }

    // Compare detected conflicts
    if (!reachedState.getDetectedConflicts().containsAll(this.getDetectedConflicts())) {
      return false;
    }

    return true;
  }

  // TODO: implement me or change me in checkProperty()
  private boolean isUnsequenced() {
    return false;
  }

  @Override
  public String getCPAName() {
    // Keep this in sync with config/specification/deterministic-execution-order.spc
    return "UnseqBehaviorAnalysisCPA";
  }

  // Flags the current state as target state if it returns true. Is automatically queried.
  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(UNSEQUENCED) && isUnsequenced()) {
      // Unsequenced/Unspecified behavior found according to C11 standard annex J.
      // TODO: give more information
      // logger.log(Level.FINE, "Found possible unsequenced execution order ...");
      return true;
    }
    return false;
  }
}
