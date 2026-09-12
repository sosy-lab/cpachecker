// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import java.math.BigInteger;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/** Shared paths through a block, paired with the condition that made their endpoint relevant. */
final class ArgPathAndCondition {

  private final DssBlockPathGraph graph;
  private final ARGState target;
  private final @Nullable ARGState condition;

  ArgPathAndCondition(DssBlockPathGraph pGraph, ARGState pTarget, @Nullable ARGState pCondition) {
    graph = pGraph;
    target = pTarget;
    condition = pCondition;
  }

  Iterable<ARGPath> paths() {
    return graph.pathsTo(target);
  }

  BigInteger pathCount() {
    return graph.pathCount(target);
  }

  @Nullable ARGState condition() {
    return condition;
  }

  @Override
  public int hashCode() {
    return Objects.hash(graph, target, condition);
  }

  // Snapshots of the same endpoints can contain different paths after a late arrival.
  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof ArgPathAndCondition other
            && graph == other.graph
            && target == other.target
            && condition == other.condition);
  }

  @Override
  public String toString() {
    return "ArgPathAndCondition{paths="
        + pathCount()
        + ", target="
        + target.getStateId()
        + ", condition="
        + condition
        + '}';
  }
}
