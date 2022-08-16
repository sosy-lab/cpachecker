// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

// cannot be an AbstractStateWithLocation as initialization corrupts analysis
public class BlockState
    implements AbstractQueryableState,
        Partitionable,
        Serializable,
        Targetable,
        FormulaReportingState {

  private static final long serialVersionUID = 3805801L;

  public enum BlockStateType {
    INITIAL,
    MID,
    FINAL
  }

  private final CFANode targetCFANode;
  private final CFANode node;
  private final AnalysisDirection direction;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private final boolean wasLoopHeadEncountered;
  private final Optional<BooleanFormula> errorCondition;

  public BlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      AnalysisDirection pDirection,
      BlockStateType pType,
      boolean pWasLoopHeadEncountered,
      Optional<BooleanFormula> pErrorCondition) {
    node = pNode;
    direction = pDirection;
    type = pType;
    if (pTargetNode == null) {
      targetCFANode = CFANode.newDummyCFANode();
    } else {
      targetCFANode =
          direction == AnalysisDirection.FORWARD
              ? pTargetNode.getLastNode()
              : pTargetNode.getStartNode();
    }
    wasLoopHeadEncountered = pWasLoopHeadEncountered;
    errorCondition = pErrorCondition;
    blockNode = pTargetNode;
  }

  public BlockNode getBlockNode() {
    return blockNode;
  }

  public boolean isTargetLoopHead() {
    return targetCFANode.equals(getLocationNode());
  }

  public boolean hasLoopHeadEncountered() {
    return wasLoopHeadEncountered;
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
    return this;
  }

  @Override
  public String toString() {
    return "Location: " + node;
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return isTarget()
        ? ImmutableSet.of(new BlockEntryReachedTargetInformation(targetCFANode))
        : ImmutableSet.of();
  }

  public Optional<BooleanFormula> getErrorCondition() {
    return errorCondition;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    return isTarget() && direction == AnalysisDirection.FORWARD
        ? errorCondition.orElse(manager.getBooleanFormulaManager().makeTrue())
        : manager.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof BlockState)) {
      return false;
    }
    BlockState that = (BlockState) pO;
    return direction == that.direction
        && Objects.equals(targetCFANode, that.targetCFANode)
        && Objects.equals(node, that.node)
        && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetCFANode, node, direction, type);
  }

  @Override
  public boolean isTarget() {
    return targetCFANode.equals(node);
  }
}
