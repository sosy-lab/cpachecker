// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.FluentIterable;
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
    FINAL,
    ABSTRACTION
  }

  private final CFANode targetCFANode;
  private final CFANode node;
  private final AnalysisDirection direction;
  private final BlockStateType type;
  private final BlockNode blockNode;
  private Optional<AbstractState> errorCondition;

  public BlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      AnalysisDirection pDirection,
      BlockStateType pType,
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
    blockNode = pTargetNode;
    errorCondition = pErrorCondition;
  }

  public void setErrorCondition(Optional<AbstractState> pErrorCondition) {
    errorCondition = pErrorCondition;
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
    return this;
  }

  @Override
  public String toString() {
    return "BlockState{" + "node=" + node + ", type=" + type + '}';
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return isTarget()
        ? ImmutableSet.of(
            new BlockEntryReachedTargetInformation(
                targetCFANode, type == BlockStateType.ABSTRACTION))
        : ImmutableSet.of();
  }

  public Optional<AbstractState> getErrorCondition() {
    return errorCondition;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    if (isTarget()
        && errorCondition.isPresent()
        && direction == AnalysisDirection.FORWARD
        && !blockNode.getLastNode().equals(blockNode.getAbstractionNode())
        && !blockNode.getLastNode().equals(node)
        && !isStartNodeOfBlock()) {
      FluentIterable<BooleanFormula> approximations =
          AbstractStates.asIterable(errorCondition.orElseThrow())
              .filter(FormulaReportingState.class)
              .transform(s -> s.getFormulaApproximation(manager));
      return manager.getBooleanFormulaManager().and(approximations.toList());
    }
    return manager.getBooleanFormulaManager().makeTrue();
  }

  private BooleanFormula extractFormula(
      PredicateAbstractState pPredicateAbstractState, FormulaManagerView manager) {
    if (pPredicateAbstractState.isAbstractionState()) {
      // already uninstantiated by convention
      return pPredicateAbstractState.getAbstractionFormula().asFormula();
    }
    return PredicateOperatorUtil.uninstantiate(pPredicateAbstractState.getPathFormula(), manager)
        .booleanFormula();
  }

  // error condition intentionally left out as it is mutable
  @Override
  public boolean equals(Object pO) {
    if (pO instanceof BlockState that) {
      return direction == that.direction
          && Objects.equals(targetCFANode, that.targetCFANode)
          && Objects.equals(node, that.node)
          && type == that.type;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetCFANode, node, direction, type);
  }

  private boolean isLocatedOnTargetNode() {
    // equal to isLastNodeOfBlock iff there is no artificial block end,
    // else true iff node is equal to artificial block end
    return targetCFANode.equals(node);
  }

  private boolean isLastNodeOfBlock() {
    // abstract at the real block end (last node, not artificial block end)
    return blockNode.getLastNode().equals(node);
  }

  private boolean isStartNodeOfBlock() {
    return blockNode.getStartNode().equals(node);
  }

  @Override
  public boolean isTarget() {
    return isLocatedOnTargetNode()
        || (direction == AnalysisDirection.FORWARD ? isLastNodeOfBlock() : isStartNodeOfBlock());
  }
}
