// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.ViolationConditionReportingState;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

// cannot be an AbstractStateWithLocation as initialization corrupts analysis
public class BlockState
    implements AbstractQueryableState, Partitionable, Targetable, FormulaReportingState {

  public enum BlockStateType {
    INITIAL,
    MID,
    FINAL,
    ABSTRACTION
  }

  private final CFANode node;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private final ImmutableList<String> history;
  private List<? extends AbstractState> violationConditions;

  public BlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      List<? extends AbstractState> pViolationConditions,
      List<String> pHistory) {
    node = pNode;
    type = pType;
    blockNode = pTargetNode;
    violationConditions = pViolationConditions;
    history = ImmutableList.copyOf(pHistory);
  }

  public ImmutableList<String> getHistory() {
    return history;
  }

  public void setViolationConditions(List<? extends AbstractState> pViolationConditions) {
    violationConditions = pViolationConditions;
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
    return blockNode;
  }

  @Override
  public String toString() {
    return "BlockState{" + "node=" + node + ", type=" + type + '}';
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
    if (isTarget()) {
      ImmutableList.Builder<BooleanFormula> combined = ImmutableList.builder();
      for (AbstractState violationCondition : violationConditions) {
        FluentIterable<BooleanFormula> approximations =
            AbstractStates.asIterable(violationCondition)
                .filter(ViolationConditionReportingState.class)
                .transform(s -> s.getViolationCondition(manager));
        combined.add(manager.getBooleanFormulaManager().and(approximations.toList()));
      }
      return manager.getBooleanFormulaManager().or(combined.build());
    }
    return manager.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public BooleanFormula getScopedFormulaApproximation(
      FormulaManagerView manager, FunctionEntryNode functionScope) {
    throw new UnsupportedOperationException();
  }

  // error condition intentionally left out as it is mutable
  @Override
  public boolean equals(Object pO) {
    return pO instanceof BlockState that
        && Objects.equals(node, that.node)
        && type == that.type
        && blockNode == that.getBlockNode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, type);
  }

  @Override
  public boolean isTarget() {
    return !violationConditions.isEmpty() && node.equals(blockNode.getViolationConditionLocation());
  }
}
