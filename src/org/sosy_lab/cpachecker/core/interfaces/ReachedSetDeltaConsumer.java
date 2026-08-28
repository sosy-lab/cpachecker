// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet.ReachedSetDelta;

/**
 * Optional capability of a {@link Refiner} whose decisions depend on how the reached set evolved
 * over the refinement rounds performed so far.
 *
 * <p>Implementations are consumers of the tracking data only. They neither own nor control the
 * tracking; the calling algorithm supplies the history before every refinement. A refiner
 * implementing this interface signals to the verification algorithm that tracking should be
 * enabled, ensuring that refiners that do not need deltas cause no tracking overhead.
 */
public interface ReachedSetDeltaConsumer {

  /**
   * Supplies the deltas of the tracking windows closed so far, oldest first. Called by the driving
   * algorithm immediately before every call to {@link Refiner#performRefinement}.
   *
   * @param pDeltaHistory the history of changes in a reached set, never null, possibly shorter than
   *     the number of refinements performed so far because the session bounds its history
   */
  void consumeDeltaHistory(ImmutableList<ReachedSetDelta> pDeltaHistory);
}
