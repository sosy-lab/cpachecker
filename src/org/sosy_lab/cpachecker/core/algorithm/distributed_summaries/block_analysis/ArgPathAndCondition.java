// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/**
 * An ARG path through a block together with the violation condition that made this path relevant.
 *
 * <p>The condition is {@code null} if the path leads to a violation that originates inside this
 * block instead of a violation condition received from a successor block.
 */
public final class ArgPathAndCondition {

  private final ARGPath path;
  private final @Nullable ARGState condition;

  // Precomputed once because ARGPath/ARGState are immutable and computing the id iterates the
  // full path; caching avoids recomputation on every hashCode/equals call.
  private final String id;

  ArgPathAndCondition(ARGPath pPath, @Nullable ARGState pCondition) {
    path = pPath;
    condition = pCondition;
    id =
        FluentIterable.from(pPath.getFullPath())
            .transform(edge -> edge.getPredecessor() + "->" + edge.getSuccessor())
            .join(Joiner.on(", "));
  }

  public ARGPath path() {
    return path;
  }

  public @Nullable ARGState condition() {
    return condition;
  }

  @Override
  public int hashCode() {
    // ARGState inherits equals/hashCode from Object, so hashing the condition directly is
    // consistent with the identity comparison performed in equals(Object).
    return Objects.hash(id, condition);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof ArgPathAndCondition other
        && Objects.equals(id, other.id)
        && Objects.equals(condition, other.condition);
  }

  @Override
  public String toString() {
    return "ArgPathAndCondition{path=" + id + ", condition=" + condition + '}';
  }
}
