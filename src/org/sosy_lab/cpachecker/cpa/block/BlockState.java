// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.PredicateOperatorUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

// cannot be an AbstractStateWithLocation as initialization corrupts analysis
public class BlockState
    implements AbstractQueryableState, Partitionable, Targetable, FormulaReportingState {

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
  private Optional<AbstractState> errorCondition;

  public BlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      AnalysisDirection pDirection,
      BlockStateType pType,
      boolean pWasLoopHeadEncountered,
      Optional<AbstractState> pErrorCondition) {
    node = pNode;
    direction = pDirection;
    type = pType;
    if (pTargetNode == null) {
      targetCFANode = CFANode.newDummyCFANode();
    } else {
      targetCFANode =
          direction == AnalysisDirection.FORWARD
              ? pTargetNode.getAbstractionNode()
              : pTargetNode.getStartNode();
    }
    wasLoopHeadEncountered = pWasLoopHeadEncountered;
    blockNode = pTargetNode;
    errorCondition = pErrorCondition;
  }

  public void setErrorCondition(Optional<AbstractState> pErrorCondition) {
    errorCondition = pErrorCondition;
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
    return "BlockState{" + "node=" + node + ", type=" + type + '}';
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return isTarget()
        ? ImmutableSet.of(new BlockEntryReachedTargetInformation(targetCFANode))
        : ImmutableSet.of();
  }

  public Optional<AbstractState> getErrorCondition() {
    return errorCondition;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    return isTarget()
            && direction == AnalysisDirection.FORWARD
            && !blockNode.getLastNode().equals(blockNode.getAbstractionNode())
        ? errorCondition
            .map(
                state ->
                    PredicateOperatorUtil.uninstantiate(
                            Objects.requireNonNull(
                                    AbstractStates.extractStateByType(
                                        state, PredicateAbstractState.class))
                                .getPathFormula(),
                            manager)
                        .booleanFormula())
            .orElse(manager.getBooleanFormulaManager().makeTrue())
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
