// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockNodeWithoutGraphInformation {
  private final String id;
  private final CFANode first;
  private final CFANode last;
  private final ImmutableSet<CFANode> nodes;
  private final ImmutableSet<CFAEdge> edges;
  private final String code;

  public BlockNodeWithoutGraphInformation(
      @NonNull String pId,
      @NonNull CFANode pFirst,
      @NonNull CFANode pLast,
      @NonNull ImmutableSet<CFANode> pNodes,
      @NonNull ImmutableSet<CFAEdge> pEdges) {
    id = pId;
    first = pFirst;
    last = pLast;
    nodes = pNodes;
    edges = pEdges;
    code = getCodeRepresentation();
  }

  public String getId() {
    return id;
  }

  public CFANode getFirst() {
    return first;
  }

  public CFANode getAbstractionLocation() {
    return last;
  }

  public CFANode getLast() {
    return last;
  }

  public ImmutableSet<CFANode> getNodes() {
    return nodes;
  }

  public String getCode() {
    return code;
  }

  public ImmutableSet<CFAEdge> getEdges() {
    return edges;
  }

  public boolean isEmpty() {
    return getEdges().isEmpty();
  }

  /**
   * Compute the code that this block contains (for debugging only).
   *
   * @return code represented by this block
   */
  private String getCodeRepresentation() {
    StringBuilder codeLines = new StringBuilder();
    for (CFAEdge leavingEdge : getEdges()) {
      if (leavingEdge.getCode().isBlank()) {
        continue;
      }
      if (leavingEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        codeLines.append("[").append(leavingEdge.getCode()).append("]\n");
      } else {
        codeLines.append(leavingEdge.getCode()).append("\n");
      }
    }
    return codeLines.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BlockNodeWithoutGraphInformation that) {
      return id.equals(that.getId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "BlockNodeWithoutGraphInformation["
        + "id="
        + id
        + ", "
        + "first="
        + first
        + ", "
        + "last="
        + last
        + ", "
        + "nodes="
        + nodes
        + ", "
        + "code="
        + code
        + ']';
  }
}
