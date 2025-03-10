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
  private final CFANode initialLocation;
  private final CFANode finalLocation;
  private final ImmutableSet<CFANode> nodes;
  private final ImmutableSet<CFAEdge> edges;
  private final String code;

  public BlockNodeWithoutGraphInformation(
      @NonNull String pId,
      @NonNull CFANode pFirst,
      @NonNull CFANode pFinalLocation,
      @NonNull ImmutableSet<CFANode> pNodes,
      @NonNull ImmutableSet<CFAEdge> pEdges) {
    id = pId;
    initialLocation = pFirst;
    finalLocation = pFinalLocation;
    nodes = pNodes;
    edges = pEdges;
    code = getCodeRepresentation();
  }

  public String getId() {
    return id;
  }

  public CFANode getInitialLocation() {
    return initialLocation;
  }

  public CFANode getViolationConditionLocation() {
    return finalLocation;
  }

  public CFANode getFinalLocation() {
    return finalLocation;
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
    String codeExtraLine = codeLines.toString();
    if (codeExtraLine.isBlank()) {
      return "";
    }
    return codeExtraLine.substring(0, codeExtraLine.length() - 1);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof BlockNodeWithoutGraphInformation other && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass(), id);
  }

  @Override
  public String toString() {
    String niceCode = code.isBlank() ? "" : "code=" + code;
    return "BlockNodeWithoutGraphInformation["
        + "id="
        + id
        + ", "
        + "first="
        + initialLocation
        + ", "
        + "last="
        + finalLocation
        + ", "
        + "nodes="
        + nodes
        + ", "
        + niceCode
        + ']';
  }
}
