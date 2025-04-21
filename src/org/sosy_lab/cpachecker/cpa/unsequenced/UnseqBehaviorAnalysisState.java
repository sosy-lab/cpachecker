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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

public class UnseqBehaviorAnalysisState implements AbstractState, Graphable {

  private final Map<String, Set<SideEffectInfo>> sideEffectsInFun;
  private boolean isFunctionCalled;
  private String calledFunctionName;
  private final Set<ConflictPair> detectedConflicts;
  private final Map<String, String> tmpNameFunNameMap;

  public UnseqBehaviorAnalysisState(
      Map<String, Set<SideEffectInfo>> pSideEffectsInFun,
      boolean pIsFunctionCalled,
      String pCalledFunctionName,
      Set<ConflictPair> pDetectedConflicts,
      Map<String, String> pTmpNameFunNameMap) {
    sideEffectsInFun = pSideEffectsInFun;
    isFunctionCalled = pIsFunctionCalled;
    calledFunctionName = pCalledFunctionName;
    detectedConflicts = pDetectedConflicts;
    tmpNameFunNameMap = pTmpNameFunNameMap;
  }

  public UnseqBehaviorAnalysisState() {
    sideEffectsInFun = new HashMap<>();
    isFunctionCalled = false;
    calledFunctionName = null;
    detectedConflicts = new HashSet<>();
    tmpNameFunNameMap = new HashMap<>();
  }

  public Map<String, Set<SideEffectInfo>> getSideEffectsInFun() {
    return sideEffectsInFun;
  }

  public void addSideEffectsToFunction(String functionName, Set<SideEffectInfo> newEffects) {
    sideEffectsInFun
        .computeIfAbsent(functionName, k -> new HashSet<>())
        .addAll(newEffects);
  }

  public void addConflicts(Set<ConflictPair> conflicts) {
    detectedConflicts.addAll(conflicts);
  }

  public Set<ConflictPair> getDetectedConflicts() {
    return detectedConflicts;
  }

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

  public void mapTmpToFunction(String tmpVar, String functionName) {
    tmpNameFunNameMap.put(tmpVar, functionName);
  }

  public String getFunctionForTmp(String tmpVar) {
    return tmpNameFunNameMap.get(tmpVar);
  }

  public void clearTmpMappings() {
    tmpNameFunNameMap.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UnseqBehaviorAnalysisState other)) return false;
    return isFunctionCalled == other.isFunctionCalled
        && Objects.equals(calledFunctionName, other.calledFunctionName)
        && Objects.equals(sideEffectsInFun, other.sideEffectsInFun)
        && Objects.equals(detectedConflicts, other.detectedConflicts)
        && Objects.equals(tmpNameFunNameMap, other.tmpNameFunNameMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        sideEffectsInFun,
        isFunctionCalled,
        calledFunctionName,
        detectedConflicts,
        tmpNameFunNameMap
    );

  }

  public String printConflict() {
    if (detectedConflicts.isEmpty()) {
      return "conflicts[]";
    }

    StringBuilder sb = new StringBuilder("conflicts[");

    boolean first = true;
    for (ConflictPair conflict : detectedConflicts) {
      CFAEdge edge = conflict.getLocation();
      String stmt = edge.getRawStatement();
      int line = edge.getFileLocation().getStartingLineInOrigin();

      String accessA = conflict.getAccessA().toStringSimple();
      String accessB = conflict.getAccessB().toStringSimple();

      if (!first) {
        sb.append(", ");
      } else {
        first = false;
      }

      sb.append(String.format(
          "[%s, line: %d, %s, %s]",
          stmt, line, accessA, accessB));
    }

    sb.append("]");
    return sb.toString();
  }

  public String printSideEffectsInFun() {
    if (sideEffectsInFun.isEmpty()) {
      return "SideEffectsInFun[]";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("SideEffectsInFun[");

    boolean firstEffect = true;
    for (Map.Entry<String, Set<SideEffectInfo>> entry : sideEffectsInFun.entrySet()) {
      String functionName = entry.getKey();
      for (SideEffectInfo effect : entry.getValue()) {
        if (!firstEffect) {
          sb.append(", ");
        } else {
          firstEffect = false;
        }

        sb.append("[").append(functionName).append(": ").append(effect.toStringSimple()).append("]");
      }
    }

    sb.append("]");
    return sb.toString();
  }


  @Override
  public String toDOTLabel() {
    return printConflict() +
        printSideEffectsInFun();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
