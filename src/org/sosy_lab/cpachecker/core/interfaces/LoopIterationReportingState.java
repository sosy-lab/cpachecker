// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * A state that reports how often the paths it represents iterated through the loops.
 *
 * <p>Usually, all paths represented by a state have the same iteration counts. A state merged from
 * paths with different iteration counts reports the smallest counts of these paths via {@link
 * #getIteration(Loop)} and {@link #getDeepestIteration()}, and the largest ones via the
 * corresponding upper-bound methods.
 */
public interface LoopIterationReportingState extends AbstractState, Partitionable {

  int getIteration(Loop pLoop);

  int getDeepestIteration();

  Set<Loop> getDeepestIterationLoops();

  default int getIterationUpperBound(Loop pLoop) {
    return getIteration(pLoop);
  }

  default int getDeepestIterationUpperBound() {
    return getDeepestIteration();
  }

  /** Returns whether the paths represented by this state may have different iteration counts. */
  default boolean hasIterationRange() {
    return false;
  }
}
