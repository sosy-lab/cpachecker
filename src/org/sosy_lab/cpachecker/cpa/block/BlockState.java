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
    ABSTRACTION,
    WITNESS
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
    Preconditions.checkArgument(
        pType == BlockStateType.WITNESS || pWitnessCheckPathState == null,
        "Added path state while not being in Witnes state");
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
    return "BlockState{ type="
        + type
        + (type == BlockStateType.WITNESS
            ? (", pathState=" + witnessCheckPathState.orElseThrow())
            : (", node=" + node))
        + '}';
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

  @Override
  public boolean equals(Object other) {
    return this == other
        || (other instanceof BlockState that
            && Objects.equals(node, that.node)
            && Objects.equals(witnessCheckPathState, that.witnessCheckPathState)
            && type == that.type
            && blockNode == that.getBlockNode()
            && violationConditions == that.violationConditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, witnessCheckPathState, type, blockNode, violationConditions);
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
