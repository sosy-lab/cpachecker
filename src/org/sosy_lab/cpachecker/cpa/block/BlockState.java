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
  private Optional<AbstractState> errorCondition;

  public BlockState(
      CFANode pNode,
      BlockNode pTargetNode,
      BlockStateType pType,
      Optional<AbstractState> pErrorCondition) {
    node = pNode;
    type = pType;
    blockNode = pTargetNode;
    errorCondition = pErrorCondition;
  }

  public void setErrorCondition(AbstractState pErrorCondition) {
    errorCondition = Optional.of(pErrorCondition);
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
                blockNode.getAbstractionLocation(), type == BlockStateType.ABSTRACTION))
        : ImmutableSet.of();
  }

  public Optional<AbstractState> getErrorCondition() {
    return errorCondition;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    if (isTarget() && errorCondition.isPresent()) {
      FluentIterable<BooleanFormula> approximations =
          AbstractStates.asIterable(errorCondition.orElseThrow())
              .filter(FormulaReportingState.class)
              .transform(s -> s.getFormulaApproximation(manager));
      return manager.getBooleanFormulaManager().and(approximations.toList());
    }
    return manager.getBooleanFormulaManager().makeTrue();
  }

  // error condition intentionally left out as it is mutable
  @Override
  public boolean equals(Object pO) {
    if (pO instanceof BlockState that) {
      return Objects.equals(node, that.node) && type == that.type;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, type);
  }

  @Override
  public boolean isTarget() {
    return blockNode.getAbstractionLocation().equals(node) && blockNode.isAbstractionPossible();
  }
}
