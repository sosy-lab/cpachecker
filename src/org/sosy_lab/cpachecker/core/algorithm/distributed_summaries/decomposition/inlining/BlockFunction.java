// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public record BlockFunction(
    String name,
    ImmutableSet<BlockNode> blockNodes,
    BlockNode entryNode,
    ImmutableSet<BlockNode> exitNodes) {

  @Override
  public int hashCode() {
    return Objects.hash(entryNode);
  }

  /** simplified equality check: only compare the first block, as it is unique */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof BlockFunction other && entryNode.equals(other.entryNode);
  }

  @Override
  public String toString() {
    return name;
  }
}
