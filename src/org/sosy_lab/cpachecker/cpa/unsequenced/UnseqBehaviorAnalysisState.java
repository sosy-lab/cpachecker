// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class UnseqBehaviorAnalysisState
    implements LatticeAbstractState<UnseqBehaviorAnalysisState>, Graphable {

  private final Map<String, Set<SideEffectInfo>> sideEffectsInFun; //total side effects
  private boolean isFunctionCalled;
  private String calledFunctionName;
  private final Set<ConflictPair> detectedConflicts;
  private final Map<String, CRightHandSide> tmpToOriginalExprMap;

  public UnseqBehaviorAnalysisState(
      Map<String, Set<SideEffectInfo>> pSideEffectsInFun,
      boolean pIsFunctionCalled,
      String pCalledFunctionName,
      Set<ConflictPair> pDetectedConflicts,
      Map<String, CRightHandSide> pTmpToOriginalExprMap) {
    sideEffectsInFun = pSideEffectsInFun;
    isFunctionCalled = pIsFunctionCalled;
    calledFunctionName = pCalledFunctionName;
    detectedConflicts = pDetectedConflicts;
    tmpToOriginalExprMap = pTmpToOriginalExprMap;
  }

  public UnseqBehaviorAnalysisState() {
    sideEffectsInFun = new HashMap<>();
    isFunctionCalled = false;
    calledFunctionName = null;
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
  public boolean hasFunctionCallOccurred() {
    return isFunctionCalled;
  }

  public String getCalledFunctionName() {
    return calledFunctionName;
  }

  public void setCalledFunctionName(String pCalledFunctionName) {
    calledFunctionName = pCalledFunctionName;
  }

  public void setFunctionCalled(boolean pFunctionCalled) {
    isFunctionCalled = pFunctionCalled;
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

  public void clearTmpMappings() {
    tmpToOriginalExprMap.clear();
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
    return isFunctionCalled == other.isFunctionCalled
        && Objects.equals(calledFunctionName, other.calledFunctionName)
        && Objects.equals(sideEffectsInFun, other.sideEffectsInFun)
        && Objects.equals(detectedConflicts, other.detectedConflicts)
        && Objects.equals(tmpToOriginalExprMap, other.tmpToOriginalExprMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        sideEffectsInFun,
        isFunctionCalled,
        calledFunctionName,
        detectedConflicts,
        tmpToOriginalExprMap);
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
    if (this.isFunctionCalled != other.isFunctionCalled) {
      throw new CPAException("Cannot join states with different function call status.");
    }
    if (!Objects.equals(this.calledFunctionName, other.calledFunctionName)) {
      throw new CPAException("Cannot join states with different called function names.");
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

    newState.setFunctionCalled(this.isFunctionCalled);
    newState.setCalledFunctionName(this.calledFunctionName);

    return newState;
  }

  @Override
  public boolean isLessOrEqual(UnseqBehaviorAnalysisState reachedState)
      throws CPAException, InterruptedException {
    // Compare function call status
    if (this.isFunctionCalled != reachedState.isFunctionCalled) {
      return false;
    }

    // Compare called function name
    if (!Objects.equals(this.calledFunctionName, reachedState.calledFunctionName)) {
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
}
