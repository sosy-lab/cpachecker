// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.SideEffectKind;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class UnseqBehaviorAnalysisState
    implements LatticeAbstractState<UnseqBehaviorAnalysisState>, Graphable, AbstractQueryableState {

  // Property
  private static final String UNSEQUENCED = "has-unsequenced-execution";

  private final ImmutableMap<String, ImmutableSet<SideEffectInfo>>
      sideEffectsInFun; // total side effects
  private final ImmutableList<String> calledFunctionStack;
  private final ImmutableSet<ConflictPair> detectedConflicts;
  private final ImmutableMap<String, CRightHandSide> tmpToOriginalExprMap;
  private final LogManager logger;

  public UnseqBehaviorAnalysisState(
      ImmutableMap<String, ImmutableSet<SideEffectInfo>> pSideEffectsInFun,
      ImmutableList<String> pCalledFunctionStack,
      ImmutableSet<ConflictPair> pDetectedConflicts,
      ImmutableMap<String, CRightHandSide> pTmpToOriginalExprMap,
      LogManager pLogger) {
    sideEffectsInFun = pSideEffectsInFun;
    calledFunctionStack = pCalledFunctionStack;
    detectedConflicts = pDetectedConflicts;
    tmpToOriginalExprMap = pTmpToOriginalExprMap;
    logger = pLogger;
  }

  public static UnseqBehaviorAnalysisState empty(LogManager pLogger) {
    return new UnseqBehaviorAnalysisState(
        ImmutableMap.of(), ImmutableList.of(), ImmutableSet.of(), ImmutableMap.of(), pLogger);
  }

  // === Side effect management ===
  public ImmutableMap<String, ImmutableSet<SideEffectInfo>> getSideEffectsInFun() {
    return sideEffectsInFun;
  }

  public ImmutableSet<SideEffectInfo> getAllPointerSideEffects() {
    return sideEffectsInFun.values().stream()
        .flatMap(Set::stream)
        .filter(se -> se.sideEffectKind() == SideEffectKind.POINTER_DEREFERENCE_UNRESOLVED)
        .collect(ImmutableSet.toImmutableSet());
  }

  public ImmutableSet<MemoryLocation> getAllMemoryLocations() {
    return sideEffectsInFun.values().stream()
        .flatMap(Collection::stream)
        .map(SideEffectInfo::memoryLocation)
        .collect(ImmutableSet.toImmutableSet());
  }

  // === Conflict tracking ===
  public ImmutableSet<ConflictPair> getDetectedConflicts() {
    return detectedConflicts;
  }

  // === Function call tracking ===
  public boolean isInsideFunctionCall() {
    return !calledFunctionStack.isEmpty();
  }

  public ImmutableList<String> getCalledFunctionStack() {
    return calledFunctionStack;
  }

  // === TMP to expression mapping ===
  public ImmutableMap<String, CRightHandSide> getTmpToOriginalExprMap() {
    return tmpToOriginalExprMap;
  }

  public Optional<CRightHandSide> getFunctionForTmp(String tmpVar) {
    return Optional.ofNullable(tmpToOriginalExprMap.get(tmpVar));
  }

  @Override
  public boolean equals(Object pOther) {
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

  public LogManager getLogger() {
    return logger;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        sideEffectsInFun, calledFunctionStack, detectedConflicts, tmpToOriginalExprMap);
  }

  public String formatConflictsInline() {
    StringBuilder sb = new StringBuilder();
    sb.append(detectedConflicts.size()).append(" conflicts [");

    int index = 1;
    for (ConflictPair conflict : detectedConflicts) {
      sb.append(conflict.toString());
      if (index < detectedConflicts.size()) {
        sb.append(", ");
      }
      index++;
    }

    sb.append("]");
    return sb.toString();
  }

  @Override
  public String toDOTLabel() {
    return formatConflictsInline();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public UnseqBehaviorAnalysisState join(UnseqBehaviorAnalysisState other)
      throws CPAException, InterruptedException {
    if (this.equals(other)) {
      return this;
    }
    if (!Objects.equals(this.calledFunctionStack, other.calledFunctionStack)) {
      throw new CPAException("Cannot join states with different function call stacks.");
    }

    Map<String, Set<SideEffectInfo>> mutableSideEffects = new HashMap<>();
    for (Map.Entry<String, ImmutableSet<SideEffectInfo>> entry : this.sideEffectsInFun.entrySet()) {
      mutableSideEffects.put(entry.getKey(), new HashSet<>(entry.getValue()));
    }
    for (Map.Entry<String, ImmutableSet<SideEffectInfo>> entry :
        other.sideEffectsInFun.entrySet()) {
      mutableSideEffects.merge(
          entry.getKey(),
          new HashSet<>(entry.getValue()),
          (a, b) -> {
            for (SideEffectInfo se : b) {
              boolean exists =
                  a.stream()
                      .anyMatch(
                          x ->
                              Objects.equals(
                                      x.cfaEdge().getLineNumber(), se.cfaEdge().getLineNumber())
                                  && x.sideEffectKind() == se.sideEffectKind()
                                  && Objects.equals(x.memoryLocation(), se.memoryLocation()));
              if (!exists) {
                a.add(se);
              }
            }
            return a;
          });
    }
    ImmutableMap<String, ImmutableSet<SideEffectInfo>> mergedSideEffects =
        UnseqUtils.toImmutableSideEffectsMap(mutableSideEffects);

    return new UnseqBehaviorAnalysisState(
        mergedSideEffects,
        this.calledFunctionStack,
        this.detectedConflicts,
        this.tmpToOriginalExprMap,
        this.logger);
  }

  @Override
  public boolean isLessOrEqual(UnseqBehaviorAnalysisState reachedState)
      throws CPAException, InterruptedException {
    if (!Objects.equals(this.calledFunctionStack, reachedState.calledFunctionStack)) {
      return false;
    }

    for (Map.Entry<String, ImmutableSet<SideEffectInfo>> entry : this.sideEffectsInFun.entrySet()) {
      String functionName = entry.getKey();
      Set<SideEffectInfo> thisEffects = entry.getValue();
      Set<SideEffectInfo> reachedStateEffects = reachedState.sideEffectsInFun.get(functionName);

      if (reachedStateEffects == null) {
        return false;
      }

      for (SideEffectInfo se : thisEffects) {
        boolean covered =
            reachedStateEffects.stream()
                .anyMatch(
                    x ->
                        Objects.equals(x.cfaEdge().getLineNumber(), se.cfaEdge().getLineNumber())
                            && x.sideEffectKind() == se.sideEffectKind()
                            && Objects.equals(x.memoryLocation(), se.memoryLocation()));
        if (!covered) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean isUnsequenced() {
    return !this.getDetectedConflicts().isEmpty();
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
      return true;
    }
    return false;
  }
}
