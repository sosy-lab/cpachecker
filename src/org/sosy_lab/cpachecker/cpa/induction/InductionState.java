// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.ComponentResettingState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.AbstractionControllingState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * The state of {@link InductionCPA}: how often the path arrived at each loop since the last
 * abstraction, and whether it is in the base case or in an induction step.
 */
public final class InductionState
    implements Targetable,
        Graphable,
        LoopIterationReportingState,
        AbstractionControllingState,
        ComponentResettingState {

  enum Kind {
    CONCRETE,
    ABSTRACTION,
    INDUCTION_START,
    /** The precision describes no abstraction here, so the path ends without a proof. */
    CUT_OFF
  }

  /** The CPAs that restart at an abstraction, given by their class names. */
  record Resets(ImmutableSet<String> atAbstraction, ImmutableSet<String> atInductionStart) {

    private static boolean contains(ImmutableSet<String> pNames, ConfigurableProgramAnalysis pCpa) {
      return pNames.contains(pCpa.getClass().getName());
    }
  }

  private final PersistentSortedMap<Loop, Integer> arrivals;
  private final PersistentSortedMap<CFANode, Integer> abstractions;
  private final boolean inInductionStep;
  private final Kind kind;
  private final boolean atLoopHead;
  private final boolean reportsTargets;

  /** The loop head and its precision for all kinds except CONCRETE. */
  private final @Nullable CFANode loopHead;

  private final @Nullable LoopPrecision loopPrecision;

  private final Resets resets;

  private InductionState(
      PersistentSortedMap<Loop, Integer> pArrivals,
      PersistentSortedMap<CFANode, Integer> pAbstractions,
      boolean pInInductionStep,
      Kind pKind,
      boolean pAtLoopHead,
      boolean pReportsTargets,
      @Nullable CFANode pLoopHead,
      @Nullable LoopPrecision pLoopPrecision,
      Resets pResets) {
    arrivals = pArrivals;
    abstractions = pAbstractions;
    inInductionStep = pInInductionStep;
    kind = pKind;
    atLoopHead = pAtLoopHead;
    reportsTargets = pReportsTargets;
    loopHead = pLoopHead;
    loopPrecision = pLoopPrecision;
    resets = pResets;
  }

  static InductionState initial(Resets pResets) {
    return new InductionState(
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        false,
        Kind.CONCRETE,
        false,
        true,
        null,
        null,
        pResets);
  }

  InductionState concrete(
      PersistentSortedMap<Loop, Integer> pArrivals, boolean pAtLoopHead, boolean pReportsTargets) {
    return new InductionState(
        pArrivals,
        abstractions,
        inInductionStep,
        Kind.CONCRETE,
        pAtLoopHead,
        pReportsTargets,
        null,
        null,
        resets);
  }

  InductionState abstraction(
      CFANode pLoopHead,
      LoopPrecision pLoopPrecision,
      boolean pStartsInductionStep,
      boolean pReportsTargets) {
    // Only finite stacks need the index, and counting it otherwise prevents merges and coverage.
    boolean countsAbstractions =
        pLoopPrecision.describedAbstractions() != LoopPrecision.ALL_ABSTRACTIONS;
    return new InductionState(
        PathCopyingPersistentTreeMap.of(),
        countsAbstractions
            ? abstractions.putAndCopy(pLoopHead, getAbstractionIndex(pLoopHead) + 1)
            : abstractions,
        pStartsInductionStep || pLoopPrecision.induction(),
        pStartsInductionStep ? Kind.INDUCTION_START : Kind.ABSTRACTION,
        true,
        pReportsTargets,
        pLoopHead,
        pLoopPrecision,
        resets);
  }

  InductionState cutOff(
      PersistentSortedMap<Loop, Integer> pArrivals,
      CFANode pLoopHead,
      LoopPrecision pLoopPrecision,
      boolean pReportsTargets) {
    return new InductionState(
        pArrivals,
        abstractions,
        inInductionStep,
        Kind.CUT_OFF,
        true,
        pReportsTargets,
        pLoopHead,
        pLoopPrecision,
        resets);
  }

  Kind getKind() {
    return kind;
  }

  public boolean isAbstraction() {
    return kind == Kind.ABSTRACTION || kind == Kind.INDUCTION_START;
  }

  public boolean isInductionStart() {
    return kind == Kind.INDUCTION_START;
  }

  public boolean isCutOff() {
    return kind == Kind.CUT_OFF;
  }

  public boolean isInInductionStep() {
    return inInductionStep;
  }

  boolean reportsTargets() {
    return reportsTargets;
  }

  PersistentSortedMap<Loop, Integer> getArrivals() {
    return arrivals;
  }

  /** The number of abstractions at the given loop head on the path. */
  public int getAbstractionIndex(CFANode pLoopHead) {
    return abstractions.getOrDefault(pLoopHead, 0);
  }

  /** The loop head of an abstraction or a cut-off. */
  public CFANode getLoopHead() {
    checkState(loopHead != null, "State %s is not at an abstraction or cut-off", this);
    return loopHead;
  }

  /** The precision of the loop of an abstraction or a cut-off at the time it was created. */
  public LoopPrecision getLoopPrecision() {
    checkState(loopPrecision != null, "State %s is not at an abstraction or cut-off", this);
    return loopPrecision;
  }

  /** Whether the successors of this state report the targets they reach. */
  boolean successorsReportTargets(InductionPrecision pPrecision) {
    if (!inInductionStep) {
      return true;
    }
    for (Map.Entry<Loop, Integer> entry : arrivals.entrySet()) {
      if (entry.getValue() == pPrecision.get(entry.getKey()).k()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Optional<Boolean> shouldComputeAbstraction() {
    return atLoopHead ? Optional.of(isAbstraction()) : Optional.empty();
  }

  @Override
  public boolean mustCoverEveryVisit() {
    return kind == Kind.INDUCTION_START;
  }

  @Override
  public boolean resets(ConfigurableProgramAnalysis pCpa) {
    return switch (kind) {
      case ABSTRACTION -> Resets.contains(resets.atAbstraction(), pCpa);
      case INDUCTION_START ->
          Resets.contains(resets.atAbstraction(), pCpa)
              || Resets.contains(resets.atInductionStart(), pCpa);
      case CONCRETE, CUT_OFF -> false;
    };
  }

  @Override
  public boolean isTarget() {
    return kind == Kind.CUT_OFF;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() {
    checkState(isTarget());
    return SimpleTargetInformation.singleton(
        "precision describes no abstraction of loop head " + loopHead);
  }

  @Override
  public int getIteration(Loop pLoop) {
    return arrivals.getOrDefault(pLoop, 0);
  }

  @Override
  public int getDeepestIteration() {
    int deepest = 0;
    for (int count : arrivals.values()) {
      deepest = Math.max(deepest, count);
    }
    return deepest;
  }

  @Override
  public Set<Loop> getDeepestIterationLoops() {
    int deepest = getDeepestIteration();
    ImmutableSet.Builder<Loop> loops = ImmutableSet.builder();
    for (Map.Entry<Loop, Integer> entry : arrivals.entrySet()) {
      if (entry.getValue() == deepest) {
        loops.add(entry.getKey());
      }
    }
    return loops.build();
  }

  @Override
  public Object getPartitionKey() {
    return ImmutableList.of(arrivals, inInductionStep);
  }

  @Override
  public String toDOTLabel() {
    return kind + (inInductionStep ? " (induction step)" : "") + " " + arrivals.values();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof InductionState other
        && inInductionStep == other.inInductionStep
        && kind == other.kind
        && atLoopHead == other.atLoopHead
        && reportsTargets == other.reportsTargets
        && arrivals.equals(other.arrivals)
        && abstractions.equals(other.abstractions)
        && Objects.equals(loopHead, other.loopHead)
        && Objects.equals(loopPrecision, other.loopPrecision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        arrivals,
        abstractions,
        inInductionStep,
        kind,
        atLoopHead,
        reportsTargets,
        loopHead,
        loopPrecision);
  }

  @Override
  public String toString() {
    return "InductionState{"
        + kind
        + (inInductionStep ? ", induction step" : "")
        + ", arrivals="
        + arrivals
        + ", abstractions="
        + abstractions
        + "}";
  }
}
