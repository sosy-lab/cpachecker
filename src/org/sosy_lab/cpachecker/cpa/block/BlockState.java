// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.ViolationConditionReportingState;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

// cannot be an AbstractStateWithLocation as initialization corrupts analysis
public class BlockState
    implements AbstractQueryableState, Partitionable, Targetable, FormulaReportingState, Graphable {

  public enum BlockStateType {
    INITIAL,
    MID,
    FINAL,
    ABSTRACTION
  }

  /** Separator between the ids of the states that a combined state was created from. */
  private static final String ID_SEPARATOR = "+";

  private final String id;
  private final BlockState predecessor;
  private final CFANode node;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private BlockGraphPath history;
  private ImmutableList<? extends AbstractState> violationConditions;
  private final SegmentedPaths witness;

  private final Optional<SegmentedPaths> witnessCheckPathState;

  private final transient Set<AbstractState> hinderedByCallstack;

  /**
   * The two states that {@link #mergedWith(BlockState)} folded into this one, or an empty list if
   * this state is not the result of a merge. Compared by identity, and deliberately only the
   * immediate pair: the merge operator hands the result straight to the stop operator of {@link
   * BlockCPA}, which asks about exactly those two states and never about an earlier generation.
   */
  private final ImmutableList<BlockState> mergedFrom;

  public BlockState(
      String pId,
      BlockState pPredecessor,
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      ImmutableList<? extends AbstractState> pViolationConditions,
      BlockGraphPath pHistory,
      SegmentedPaths pWitness,
      SegmentedPaths pWitnessCheckPathState) {
    id = pId;
    predecessor = pPredecessor;
    node = pNode;
    type = pType;
    blockNode = pTargetNode;
    violationConditions = pViolationConditions;
    history = pHistory;
    witness = pWitness;
    witnessCheckPathState = Optional.ofNullable(pWitnessCheckPathState);
    hinderedByCallstack = new LinkedHashSet<>();
    mergedFrom = ImmutableList.of();
  }

  /**
   * The state that {@link #mergedWith(BlockState)} builds: a copy of {@code pReached} that records
   * both merged states.
   */
  private BlockState(BlockState pReached, BlockState pSuccessor) {
    // The reached state's id and predecessor survive. Only the block-end states of a run are
    // merged back to individual states by the algorithm (see DssBlockAnalyses), and those are
    // never merged, so nothing reads the bookkeeping of an interior state. Keeping one id instead
    // of combining them also keeps the id from growing with every merge at the same location.
    id = pReached.id;
    predecessor = pReached.predecessor;
    node = pReached.node;
    type = pReached.type;
    blockNode = pReached.blockNode;
    violationConditions = pReached.violationConditions;
    history = pReached.history;
    witness = pReached.witness;
    witnessCheckPathState = pReached.witnessCheckPathState;
    hinderedByCallstack = new LinkedHashSet<>(pReached.hinderedByCallstack);
    hinderedByCallstack.addAll(pSuccessor.hinderedByCallstack);
    mergedFrom = ImmutableList.of(pReached, pSuccessor);
  }

  public BlockState(
      String pId,
      BlockState pPredecessor,
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      ImmutableList<? extends AbstractState> pViolationConditions,
      BlockGraphPath pHistory,
      SegmentedPaths pWitness) {
    this(
        pId,
        pPredecessor,
        pNode,
        pTargetNode,
        pType,
        pViolationConditions,
        pHistory,
        pWitness,
        null);
  }

  public String getUniqueId() {
    return id;
  }

  /**
   * Joins the ids of states that are combined into a single state, so that the parts remain
   * recoverable with {@link #splitUniqueId(String)}.
   */
  public static String combineUniqueIds(Iterable<String> pIds) {
    return Joiner.on(ID_SEPARATOR).join(pIds);
  }

  /**
   * Splits an id created by {@link #combineUniqueIds(Iterable)} into the ids of the states it
   * combines. An id that does not combine several states is returned as the only element.
   */
  public static ImmutableList<String> splitUniqueId(String pId) {
    return ImmutableList.copyOf(Splitter.on(ID_SEPARATOR).split(pId));
  }

  public Set<AbstractState> getHinderedByCallstack() {
    return ImmutableSet.copyOf(hinderedByCallstack);
  }

  public void addHinderedByCallstack(AbstractState state) {
    hinderedByCallstack.add(state);
  }

  public void addHistory(BlockNode pBlockNode) {
    history = new BlockGraphPath(listAndElement(history.path(), pBlockNode.getId()));
  }

  public SegmentedPaths getWitness() {
    return witness;
  }

  public BlockGraphPath getHistory() {
    return history;
  }

  public void setViolationConditions(List<? extends AbstractState> pViolationConditions) {
    violationConditions =
        ImmutableList.sortedCopyOf(
            Comparator.comparingInt(
                v -> AbstractStates.extractStateByType(v, BlockState.class).getWitness().size()),
            pViolationConditions);
  }

  public BlockNode getBlockNode() {
    return blockNode;
  }

  public CFANode getLocationNode() {
    return node;
  }

  public BlockStateType getType() {
    return type;
  }

  @Override
  public String getCPAName() {
    return BlockCPA.class.getSimpleName();
  }

  @Override
  public @Nullable Object getPartitionKey() {
    return Objects.hash(getLocationNode(), violationConditions, type);
  }

  @Override
  public String toString() {
    return "BlockState{ type=" + type + ", node=" + node + '}';
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return isTarget()
        ? ImmutableSet.of(
            new BlockTargetInformation(
                blockNode.getViolationConditionLocation(), type == BlockStateType.ABSTRACTION))
        : ImmutableSet.of();
  }

  public ImmutableList<? extends @NonNull AbstractState> getViolationConditions() {
    return violationConditions;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    final BooleanFormulaManagerView bfmgr = manager.getBooleanFormulaManager();

    if (isTarget()) {
      ImmutableList.Builder<BooleanFormula> combined = ImmutableList.builder();
      for (AbstractState violationCondition : violationConditions) {
        FluentIterable<BooleanFormula> approximations =
            AbstractStates.asIterable(violationCondition)
                .filter(ViolationConditionReportingState.class)
                .transform(s -> s.getViolationCondition(manager));
        combined.add(bfmgr.and(approximations.toList()));
      }
      return bfmgr.or(combined.build());
    }
    return bfmgr.makeTrue();
  }

  public BlockState getPredecessor() {
    return predecessor;
  }

  @Override
  public BooleanFormula getScopedFormulaApproximation(
      FormulaManagerView manager, FunctionEntryNode functionScope) {
    throw new UnsupportedOperationException();
  }

  /**
   * Whether this state is covered by the given state, i.e., whether a block that has been analyzed
   * from {@code that} no longer has to be analyzed from this state.
   *
   * <p>This comparison deliberately ignores everything that only records where a state came from:
   * {@link #id}, {@link #predecessor}, {@link #hinderedByCallstack} and, most importantly, {@link
   * #history}. Two preconditions that reach the same block entry with the same callstack and the
   * same abstraction have to subsume each other even if they arrived along different paths through
   * the block graph, otherwise a block collects one precondition per block-graph path.
   *
   * <p>It also ignores {@link #violationConditions}, unlike {@link #equals}: coverage compares a
   * precondition that has just been deserialized from a message, which never carries a violation
   * condition, against a stored precondition that {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis
   * #runBlockAnalysis} has since set the block's current violation conditions on. Comparing the two
   * would therefore always fail, and a block would collect one precondition per arriving message
   * instead of covering the repetitions.
   */
  public boolean isCovered(BlockState that) {
    return this == that
        || (Objects.equals(node, that.node)
            && Objects.equals(witnessCheckPathState, that.witnessCheckPathState)
            && type == that.type
            && blockNode == that.getBlockNode());
  }

  /**
   * Whether the two states may be folded into one, i.e. whether nothing the distributed analysis
   * reads back from an individual state can tell them apart.
   *
   * <p>This is what lets the predicate analysis merge inside a block. Without it, a block with
   * <em>n</em> decisions keeps one state per path through it, because the composite CPA merges two
   * states only if every component agrees, and a component that never agrees vetoes the merge of
   * all the others.
   *
   * <p>Only {@link BlockStateType#MID interior} states qualify. The algorithm resolves an
   * abstraction state to the block-end state it was spawned from, and a block-end state to the
   * entry of the reached set that holds it (see {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses});
   * both look states up by identity, so replacing one of them by a merged copy would break the
   * lookup. Interior states carry no such bookkeeping. Nothing is lost by leaving the block end out
   * either: it is an abstraction location of the predicate analysis (the worker sets {@code
   * cpa.predicate.blk.alwaysAtGivenNodes} to it), and adjustable-block encoding does not merge
   * abstraction states anyway.
   *
   * <p>The violation conditions have to match as well. They decide which obligations a state still
   * has to discharge, and {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses}
   * narrows them per state as they are processed. Folding a state that still owes a condition into
   * one that does not would drop that obligation silently.
   */
  public boolean permitsMergeWith(BlockState pOther) {
    return type == BlockStateType.MID
        && pOther.type == BlockStateType.MID
        && Objects.equals(node, pOther.node)
        && blockNode == pOther.blockNode
        && Objects.equals(history, pOther.history)
        && Objects.equals(witness, pOther.witness)
        && Objects.equals(witnessCheckPathState, pOther.witnessCheckPathState)
        && violationConditions.equals(pOther.violationConditions);
  }

  /**
   * A state that stands for this state and for {@code pSuccessor}, which {@link
   * #permitsMergeWith(BlockState)} has to allow.
   *
   * <p>The receiver is the state already in the reached set, so its bookkeeping is the one that
   * survives.
   */
  BlockState mergedWith(BlockState pSuccessor) {
    Preconditions.checkArgument(
        permitsMergeWith(pSuccessor), "Merging %s into %s is not permitted", pSuccessor, this);
    return new BlockState(this, pSuccessor);
  }

  /**
   * Whether {@code pState} was folded into this state by {@link #mergedWith(BlockState)}.
   *
   * <p>This is the whole lattice of {@link BlockCPA}: a merged state is above the two states it was
   * built from, and every other pair is incomparable. Coverage therefore stays off -- the stop
   * operator must not drop a state that reached a location along its own path, because the
   * violation conditions of a block are computed per path (see {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis})
   * and a dropped path is a lost violation condition. A merge loses nothing instead: {@link
   * org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoin} gives the merged state the parents of both, so
   * every path through either state still runs through the merged one.
   */
  boolean absorbs(BlockState pState) {
    for (BlockState merged : mergedFrom) {
      if (merged == pState) {
        return true;
      }
    }
    return false;
  }

  // equals() and hashCode() are deliberately not implemented: BlockState carries bookkeeping that
  // the algorithm reads back from individual states of the reached set (the predecessor
  // back-pointer, the block-graph history, and the states that a callstack hindered), and none of
  // it would take part in a value-based comparison. Coverage is expressed by isCovered(BlockState)
  // instead, and the lattice of BlockCPA compares states by identity.

  @Override
  public boolean isTarget() {
    return !violationConditions.isEmpty()
        && node.equals(blockNode.getViolationConditionLocation())
        && blockNode.getViolationConditionLocation() != blockNode.getFinalLocation();
  }

  @Override
  public String toDOTLabel() {
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return type == BlockStateType.FINAL;
  }
}
