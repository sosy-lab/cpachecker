// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

/**
 * One state of a sequentialized counterexample path: the program location the interleaving has
 * reached, plus which thread instance took the step that led there and which instance that step
 * created, if any.
 *
 * <p>The exploration's own states cannot be reused for this. One interleaving crosses thread trees
 * and therefore has no counterpart in the forest-shaped reached set, so the algorithm builds a
 * fresh chain of these states instead (see {@code
 * OrderingConsistencyAlgorithm#attachCounterexample}). Carrying the thread instance here is what
 * lets the violation witness export label each waypoint with the thread it belongs to.
 */
public final class OcCounterexampleState
    implements AbstractStateWithLocation, OcThreadInstanceState {

  private final LocationState locationState;
  private final int instanceId;
  private final int createdInstanceId;

  OcCounterexampleState(LocationState pLocationState, int pInstanceId, int pCreatedInstanceId) {
    locationState = pLocationState;
    instanceId = pInstanceId;
    createdInstanceId = pCreatedInstanceId;
  }

  @Override
  public CFANode getLocationNode() {
    return locationState.getLocationNode();
  }

  @Override
  public int getThreadInstanceId() {
    return instanceId;
  }

  @Override
  public int getCreatedThreadInstanceId() {
    return createdInstanceId;
  }

  @Override
  public String toString() {
    return "T" + instanceId + "@" + getLocationNode();
  }
}
