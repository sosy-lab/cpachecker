// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
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

  /**
   * The CPAs that restart at an abstraction, given by their class names, and whether the memory
   * model of the predicate analysis restarts in an induction step.
   */
  record Resets(
      ImmutableSet<String> atAbstraction,
      ImmutableSet<String> inInductionStep,
      boolean memoryInInductionStep) {

    private static boolean contains(ImmutableSet<String> pNames, ConfigurableProgramAnalysis pCpa) {
      return pNames.contains(pCpa.getClass().getName());
    }
  }

  private final PersistentSortedMap<Loop, Integer> arrivals;
  private final PersistentSortedMap<CFANode, Integer> abstractions;
  private final boolean inInductionStep;
  private final Kind kind;
  private final boolean atLoopHead;

  /** Whether the predicate analysis ends a block here although the precision does not abstract. */
  private final boolean endsBlock;

  private final boolean reportsTargets;

  /** The loop head and its precision for all kinds except CONCRETE. */
  private final @Nullable CFANode loopHead;

  private final @Nullable LoopPrecision loopPrecision;

  private final Resets resets;

  /**
   * The state that started the block of this state, or null in the first block. Only the
   * abstractions of an unrolling and the ends of blocks compare it, so that they merge within a
   * block but never cover a state of another path through the loops.
   */
  private final @Nullable InductionState blockStart;

  /**
   * The precision of the split that this state continues the base case after, or null. A
   * continuation ends at the bound of its precision, so it never covers a state of another one.
   */
  private final @Nullable InductionPrecision continuedPrecision;

  /**
   * The number of arrivals at loop heads since the start of the program. It only orders the
   * exploration, so equality ignores it.
   */
  private final int depth;

  /** Whether the exploration goes level by level over all arrivals at loop heads. */
  private final boolean levelByLevel;

  /** The state of the same block that this state was merged into, or null. */
  private @Nullable InductionState mergedInto = null;

  private @LazyInit int hashCache = 0;
  private @LazyInit @Nullable PartitionKey partitionKey = null;

  /**
   * The arrivals and the phase, which states must share to cover each other. The hash is computed
   * once, because the reached set looks it up for every new state.
   */
  private record PartitionKey(
      PersistentSortedMap<Loop, Integer> arrivals, boolean inInductionStep, int hash) {

    @Override
    public boolean equals(Object pOther) {
      return pOther instanceof PartitionKey other
          && hash == other.hash
          && inInductionStep == other.inInductionStep
          && arrivals.equals(other.arrivals);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  private InductionState(
      PersistentSortedMap<Loop, Integer> pArrivals,
      PersistentSortedMap<CFANode, Integer> pAbstractions,
      boolean pInInductionStep,
      Kind pKind,
      boolean pAtLoopHead,
      boolean pEndsBlock,
      boolean pReportsTargets,
      @Nullable CFANode pLoopHead,
      @Nullable LoopPrecision pLoopPrecision,
      Resets pResets,
      @Nullable InductionState pBlockStart,
      @Nullable InductionPrecision pContinuedPrecision,
      int pDepth,
      boolean pLevelByLevel) {
    arrivals = pArrivals;
    abstractions = pAbstractions;
    inInductionStep = pInInductionStep;
    kind = pKind;
    atLoopHead = pAtLoopHead;
    endsBlock = pEndsBlock;
    reportsTargets = pReportsTargets;
    loopHead = pLoopHead;
    loopPrecision = pLoopPrecision;
    resets = pResets;
    blockStart = pBlockStart;
    continuedPrecision = pContinuedPrecision;
    depth = pDepth;
    levelByLevel = pLevelByLevel;
  }

  static InductionState initial(Resets pResets, boolean pLevelByLevel) {
    return new InductionState(
        PathCopyingPersistentTreeMap.of(),
        PathCopyingPersistentTreeMap.of(),
        false,
        Kind.CONCRETE,
        false,
        false,
        true,
        null,
        null,
        pResets,
        null,
        null,
        0,
        pLevelByLevel);
  }

  InductionState concrete(
      PersistentSortedMap<Loop, Integer> pArrivals,
      boolean pAtLoopHead,
      boolean pEndsBlock,
      boolean pReportsTargets) {
    return new InductionState(
        pArrivals,
        abstractions,
        inInductionStep,
        Kind.CONCRETE,
        pAtLoopHead,
        pEndsBlock,
        pReportsTargets,
        null,
        null,
        resets,
        blockStartOfSuccessors(),
        continuedPrecision,
        pAtLoopHead ? depth + 1 : depth,
        levelByLevel);
  }

  /** The base case after the split at a loop head, next to the induction start. */
  InductionState continuation(
      PersistentSortedMap<Loop, Integer> pArrivals,
      boolean pReportsTargets,
      InductionPrecision pPrecision) {
    return new InductionState(
        pArrivals,
        abstractions,
        inInductionStep,
        Kind.CONCRETE,
        true,
        false,
        pReportsTargets,
        null,
        null,
        resets,
        blockStartOfSuccessors(),
        pPrecision,
        depth + 1,
        levelByLevel);
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
        false,
        pReportsTargets,
        pLoopHead,
        pLoopPrecision,
        resets,
        blockStartOfSuccessors(),
        null,
        depth + 1,
        levelByLevel);
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
        false,
        pReportsTargets,
        pLoopHead,
        pLoopPrecision,
        resets,
        blockStartOfSuccessors(),
        continuedPrecision,
        depth + 1,
        levelByLevel);
  }

  private @Nullable InductionState blockStartOfSuccessors() {
    return isAbstraction() || endsBlockWithoutAbstraction() ? this : blockStart;
  }

  /** Whether the predicate analysis ends a block here although the precision does not abstract. */
  boolean endsBlockWithoutAbstraction() {
    return endsBlock;
  }

  /** Whether both states lie in the block of the same abstraction. */
  boolean isInSameBlock(InductionState pOther) {
    return blockStart == pOther.blockStart;
  }

  /** Whether this state was merged into the given state of its block. */
  boolean wasMergedInto(InductionState pOther) {
    return isInSameBlock(pOther) && mergedInto == pOther;
  }

  void setMergedInto(InductionState pOther) {
    mergedInto = pOther;
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
    return atLoopHead
        ? Optional.of(isAbstraction() || endsBlockWithoutAbstraction())
        : Optional.empty();
  }

  @Override
  public boolean mustCoverEveryVisit() {
    return kind == Kind.INDUCTION_START;
  }

  @Override
  public boolean forgetsPointerTargets() {
    return isAbstraction() && inInductionStep && resets.memoryInInductionStep();
  }

  @Override
  public boolean resets(ConfigurableProgramAnalysis pCpa) {
    return switch (kind) {
      case ABSTRACTION, INDUCTION_START ->
          Resets.contains(resets.atAbstraction(), pCpa)
              || (inInductionStep && Resets.contains(resets.inInductionStep(), pCpa));
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
    if (levelByLevel) {
      return depth;
    }
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
    PartitionKey key = partitionKey;
    if (key == null) {
      key =
          new PartitionKey(
              arrivals, inInductionStep, 31 * hashOf(arrivals) + Boolean.hashCode(inInductionStep));
      partitionKey = key;
    }
    return key;
  }

  /**
   * A hash of the counters that separates small counts, unlike {@link Map#hashCode}, which adds up
   * the entries.
   */
  private static int hashOf(PersistentSortedMap<?, Integer> pCounters) {
    int hash = 1;
    for (Map.Entry<?, Integer> entry : pCounters.entrySet()) {
      hash = 31 * (31 * hash + entry.getKey().hashCode()) + entry.getValue();
    }
    return hash;
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
        && hashCode() == other.hashCode()
        && inInductionStep == other.inInductionStep
        && kind == other.kind
        && atLoopHead == other.atLoopHead
        && endsBlock == other.endsBlock
        && reportsTargets == other.reportsTargets
        && arrivals.equals(other.arrivals)
        && abstractions.equals(other.abstractions)
        && Objects.equals(loopHead, other.loopHead)
        && Objects.equals(loopPrecision, other.loopPrecision)
        && Objects.equals(continuedPrecision, other.continuedPrecision);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache =
          Objects.hash(
              hashOf(arrivals),
              hashOf(abstractions),
              inInductionStep,
              kind,
              atLoopHead,
              endsBlock,
              reportsTargets,
              loopHead,
              loopPrecision,
              continuedPrecision);
    }
    return hashCache;
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
