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
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.ViolationConditionReportingState;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithIncomingEdge;
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
    implements AbstractQueryableState,
        Partitionable,
        Targetable,
        FormulaReportingState,
        Graphable,
        AbstractStateWithIncomingEdge {

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
  private final @Nullable CFAEdge incomingEdge;
  private final CFANode node;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private final BlockGraphPath history;
  private final ImmutableList<? extends AbstractState> violationConditions;
  private final SegmentedPaths witness;

  private final Optional<SegmentedPaths> witnessCheckPathState;

  private final BlockStateObligations obligations = new BlockStateObligations();

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
    this(
        pId,
        pPredecessor,
        pNode,
        pTargetNode,
        pType,
        pViolationConditions,
        pHistory,
        pWitness,
        pWitnessCheckPathState,
        null);
  }

  private BlockState(
      String pId,
      BlockState pPredecessor,
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      ImmutableList<? extends AbstractState> pViolationConditions,
      BlockGraphPath pHistory,
      SegmentedPaths pWitness,
      SegmentedPaths pWitnessCheckPathState,
      @Nullable CFAEdge pIncomingEdge) {
    id = pId;
    predecessor = pPredecessor;
    incomingEdge = pIncomingEdge;
    node = pNode;
    type = pType;
    blockNode = pTargetNode;
    violationConditions = pViolationConditions;
    history = pHistory;
    witness = pWitness;
    witnessCheckPathState = Optional.ofNullable(pWitnessCheckPathState);
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

  BlockState successor(
      String pId,
      CFAEdge pEdge,
      BlockStateType pType,
      ImmutableList<? extends AbstractState> pConditions) {
    return new BlockState(
        pId,
        this,
        pEdge.getSuccessor(),
        blockNode,
        pType,
        pConditions,
        history,
        witness,
        witnessCheckPathState.orElse(null),
        pEdge);
  }

  @Override
  public @Nullable CFAEdge getIncomingEdge() {
    return incomingEdge;
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
    return obligations.getHinderedByCallstack();
  }

  public void addHinderedByCallstack(AbstractState state) {
    obligations.addHinderedByCallstack(state);
  }

  public BlockState withHistory(BlockNode pBlockNode) {
    return copy(
        violationConditions,
        new BlockGraphPath(listAndElement(history.path(), pBlockNode.getId())));
  }

  public SegmentedPaths getWitness() {
    return witness;
  }

  public BlockGraphPath getHistory() {
    return history;
  }

  public BlockState withViolationConditions(List<? extends AbstractState> pViolationConditions) {
    return copy(
        ImmutableList.sortedCopyOf(
            Comparator.comparingInt(
                v -> AbstractStates.extractStateByType(v, BlockState.class).getWitness().size()),
            pViolationConditions),
        history);
  }

  private BlockState copy(
      ImmutableList<? extends AbstractState> pConditions, BlockGraphPath pHistory) {
    return new BlockState(
        id,
        predecessor,
        node,
        blockNode,
        type,
        pConditions,
        pHistory,
        witness,
        witnessCheckPathState.orElse(null),
        incomingEdge);
  }

  /** Reuse the abstract value in a new exploration without reusing processing records. */
  public BlockState reset() {
    return new BlockState(
        id,
        null,
        node,
        blockNode,
        type,
        ImmutableList.of(),
        history,
        witness,
        witnessCheckPathState.orElse(null));
  }

  /** Conditions whose ghost successors still need to be processed for this occurrence. */
  public ImmutableList<? extends AbstractState> getPendingViolationConditions() {
    return violationConditions.stream()
        .filter(condition -> !obligations.isProcessed(condition))
        .collect(ImmutableList.toImmutableList());
  }

  /** Refresh from surviving ghost states, so refinement does not leave stale completion records. */
  public void setProcessedViolationConditions(Iterable<? extends AbstractState> pConditions) {
    obligations.setProcessed(pConditions);
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
    return new BlockPartitionKey(node, type);
  }

  private record BlockPartitionKey(CFANode node, BlockStateType type) {}

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

  @Override
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
   * {@link #id}, {@link #predecessor}, {@link #obligations} and, most importantly, {@link
   * #history}. Two preconditions that reach the same block entry with the same callstack and the
   * same abstraction have to subsume each other even if they arrived along different paths through
   * the block graph, otherwise a block collects one precondition per block-graph path.
   *
   * <p>It also ignores {@link #violationConditions}, unlike {@link #equals}: these belong to one
   * local exploration, whereas distributed coverage asks whether a received entry precondition
   * needs reanalysis.
   */
  public boolean isCovered(BlockState that) {
    return this == that
        || (Objects.equals(node, that.node)
            && Objects.equals(witnessCheckPathState, that.witnessCheckPathState)
            && type == that.type
            && blockNode == that.getBlockNode());
  }

  /**
   * Equal interior values can share an ARG suffix. The ARG owns the incoming execution paths;
   * neither the diagnostic id nor the predecessor occurrence belongs to this comparison.
   *
   * <p>Boundary occurrences deliberately remain distinct. Ghost successors discharge obligations of
   * one particular block end, and merging those ends would require transferring or invalidating
   * their processing records on late merges. Initial occurrences also keep separate origins.
   * History and witnesses are immutable and compared conservatively so neither can be lost when
   * choosing a representative interior value. Distributed precondition coverage remains separate.
   */
  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof BlockState other
            && type == BlockStateType.MID
            && other.type == type
            && blockNode == other.blockNode
            && node.equals(other.node)
            && violationConditions.equals(other.violationConditions)
            && history.equals(other.history)
            && witness.equals(other.witness)
            && witnessCheckPathState.equals(other.witnessCheckPathState));
  }

  @Override
  public int hashCode() {
    return type == BlockStateType.MID
        ? Objects.hash(
            node,
            System.identityHashCode(blockNode),
            type,
            violationConditions,
            history,
            witness,
            witnessCheckPathState)
        : System.identityHashCode(this);
  }

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
