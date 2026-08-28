// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet.TrackingSession;

/**
 * Creates {@link TrackingSession}s, which provide reached sets that track their changes.
 *
 * <p>The component that calls this factory becomes the owner of the resulting session and defines
 * the meaning of a tracking window. For example, a window could correspond to the analysis work
 * performed between two iterations of an abstraction-refinement loop.
 */
public final class TrackingSessionFactory {

  private static final int DEFAULT_HISTORY_SIZE = 10;

  private TrackingSessionFactory() {}

  public static TrackingSession startTracking(ReachedSet pReached) {
    return TrackingForwardingReachedSet.createSession(pReached, DEFAULT_HISTORY_SIZE);
  }
}
