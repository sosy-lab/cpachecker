// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.base.Preconditions;
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

  @Override
  public String toDOTLabel() {
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return type == BlockStateType.FINAL;
  }

  public enum BlockStateType {
    INITIAL,
    MID,
    FINAL,
    ABSTRACTION,
    WITNESS
  }

  private final BlockState predecessor;
  private final CFANode node;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private BlockGraphPath history;
  private List<? extends AbstractState> violationConditions;
  private final SegmentedPaths witness;

  private final Optional<SegmentedPaths> witnessCheckPathState;

  public BlockState(
      BlockState pPredecessor,
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      List<? extends AbstractState> pViolationConditions,
      BlockGraphPath pHistory,
      SegmentedPaths pWitness,
      SegmentedPaths pWitnessCheckPathState) {
    Preconditions.checkArgument(
        pType == BlockStateType.WITNESS || pWitnessCheckPathState == null,
        "Added path state while not being in Witnes state");
    predecessor = pPredecessor;
    node = pNode;
    type = pType;
    blockNode = pTargetNode;
    violationConditions = ImmutableList.copyOf(pViolationConditions);
    history = pHistory;
    witness = pWitness;
    witnessCheckPathState = Optional.ofNullable(pWitnessCheckPathState);
  }

  public BlockState(
      BlockState pPredecessor,
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      List<? extends AbstractState> pViolationConditions,
      BlockGraphPath pHistory,
      SegmentedPaths pWitness) {
    this(pPredecessor, pNode, pTargetNode, pType, pViolationConditions, pHistory, pWitness, null);
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

  public List<? extends @NonNull AbstractState> getViolationConditions() {
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

  // error condition intentionally left out as it is mutable
  // the equals method is deliberately not implemented like this
  public boolean isEqualTo(BlockState that) {
    return this == that
        || (Objects.equals(node, that.node)
            && Objects.equals(witnessCheckPathState, that.witnessCheckPathState)
            && type == that.type
            && blockNode == that.getBlockNode());
  }

  @Override
  public boolean isTarget() {
    return !violationConditions.isEmpty()
        && node.equals(blockNode.getViolationConditionLocation())
        && blockNode.getViolationConditionLocation() != blockNode.getFinalLocation();
  }
}
