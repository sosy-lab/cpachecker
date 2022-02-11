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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockState
    implements AbstractStateWithLocation, AbstractQueryableState, Partitionable,
               Serializable, Targetable {

  private static final long serialVersionUID = 3805801L;

  enum BlockStateType {
    INITIAL,
    MID,
    FINAL
  }

  private final Set<TargetInformation> targetInformation;
  private final BlockNode targetNode;
  private final CFANode targetCFANode;
  private final CFANode node;
  private final AnalysisDirection direction;
  private final BlockStateType type;

  public BlockState(CFANode pNode, BlockNode pTargetNode, AnalysisDirection pDirection, BlockStateType pType) {
    node = pNode;
    targetNode = pTargetNode;
    targetInformation = new HashSet<>();
    direction = pDirection;
    type = pType;
    targetCFANode = direction == AnalysisDirection.FORWARD ? targetNode.getLastNode() : targetNode.getStartNode();
    if (isTarget()) {
      targetInformation.add(new BlockEntryReachedTargetInformation(targetCFANode));
    }
  }

  public BlockStateType getType() {
    return type;
  }

  @Override
  public CFANode getLocationNode() {
    return node;
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return ImmutableSet.of(node);
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return CFAUtils.allLeavingEdges(node);
  }

  @Override
  public Iterable<CFAEdge> getIngoingEdges() {
    return CFAUtils.allEnteringEdges(node);
  }

  @Override
  public String getCPAName() {
    return "block";
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
    return targetInformation;
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof BlockState)) {
      return false;
    }
    BlockState that = (BlockState) pO;
    return direction == that.direction && Objects.equals(targetCFANode, that.targetCFANode)
        && Objects.equals(node, that.node) && type == that.type;
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
