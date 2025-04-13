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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;



public class UnseqBehaviorAnalysisState implements AbstractState, Graphable {

  private final Map<CFAEdge, Set<SideEffectInfo>> sideEffectsByEdge;
  private final Map<String, Set<SideEffectInfo>> sideEffectsInFun;
  private boolean isFunctionCalled;
  private final Map<CFAEdge, Set<SideEffectInfo>>  detectedConflictLocations;

  public UnseqBehaviorAnalysisState(
      Map<CFAEdge, Set<SideEffectInfo>> pSideEffectsByEdge,
      Map<String, Set<SideEffectInfo>> pSideEffectsInFun,
      boolean pIsFunctionCalled,
      Map<CFAEdge, Set<SideEffectInfo>> pDetectedConflictLocations) {
    sideEffectsByEdge = pSideEffectsByEdge;
    sideEffectsInFun = pSideEffectsInFun;
    isFunctionCalled = pIsFunctionCalled;
    detectedConflictLocations = pDetectedConflictLocations;
  }

  public UnseqBehaviorAnalysisState() {
    sideEffectsByEdge = new HashMap<>();
    sideEffectsInFun = new HashMap<>();
    isFunctionCalled = false;
    detectedConflictLocations = new HashMap<>();
  }

  public Map<CFAEdge, Set<SideEffectInfo>> getSideEffectsByEdge() {
    return sideEffectsByEdge;
  }

  public Map<String, Set<SideEffectInfo>> getSideEffectsInFun() {
    return sideEffectsInFun;
  }

  public boolean hasFunctionCallOccurred() {
    return isFunctionCalled;
  }

  public Map<CFAEdge, Set<SideEffectInfo>> getDetectedConflictLocations() {
    return detectedConflictLocations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UnseqBehaviorAnalysisState)) return false;
    UnseqBehaviorAnalysisState that = (UnseqBehaviorAnalysisState) o;
    return isFunctionCalled == that.isFunctionCalled
        && Objects.equals(sideEffectsByEdge, that.sideEffectsByEdge)
        && Objects.equals(sideEffectsInFun, that.sideEffectsInFun)
        && Objects.equals(detectedConflictLocations, that.detectedConflictLocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sideEffectsByEdge, sideEffectsInFun, isFunctionCalled, detectedConflictLocations);
  }


  public void addSideEffectForEdge(CFAEdge edge, Set<SideEffectInfo> sideEffects) {
    sideEffectsByEdge.computeIfAbsent(edge, __ -> new HashSet<>())
        .addAll(sideEffects);
  }

  public void addConflict(CFAEdge edge, Set<SideEffectInfo> sideEffects) {
    detectedConflictLocations
        .computeIfAbsent(edge, __ -> new HashSet<>())
        .addAll(sideEffects);
  }


  public String printConflict() {

    if (detectedConflictLocations.isEmpty()) {
      return "No conflicts detected.";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Detected Conflicts:\n");

    for (Map.Entry<CFAEdge, Set<SideEffectInfo>> entry : detectedConflictLocations.entrySet()) {
      CFAEdge edge = entry.getKey();
      Set<SideEffectInfo> effects = entry.getValue();

      sb.append("  Conflict at: \"")
          .append(edge.getRawStatement())
          .append("\" (")
          .append(edge.getFileLocation().getNiceFileName())
          .append(":")
          .append(edge.getLineNumber())
          .append(")\n");

      for (SideEffectInfo info : effects) {
        sb.append("    → ").append(info.toStringSimple()).append("\n");
      }
    }


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
}
