// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;

public class SMGStateInformation {

  private static final SMGStateInformation EMPTY = new SMGStateInformation();

  private final ImmutableSet<SMGEdgeHasValue> hvEdges;
  private final ImmutableSet<SMGEdgePointsTo> ptEdges;
  private final boolean valid;
  private final boolean external;

  private SMGStateInformation() {
    hvEdges = ImmutableSet.of();
    ptEdges = ImmutableSet.of();
    valid = false;
    external = false;
  }

  private SMGStateInformation(
      Set<SMGEdgeHasValue> pHves,
      Set<SMGEdgePointsTo> pPtes,
      boolean pIsRegionValid,
      boolean pIsRegionExternallyAllocated) {
    hvEdges = ImmutableSet.copyOf(pHves);
    ptEdges = ImmutableSet.copyOf(pPtes);
    valid = pIsRegionValid;
    external = pIsRegionExternallyAllocated;
  }

  public static SMGStateInformation of() {
    return EMPTY;
  }

  public ImmutableSet<SMGEdgePointsTo> getPtEdges() {
    return ptEdges;
  }

  public ImmutableSet<SMGEdgeHasValue> getHvEdges() {
    return hvEdges;
  }

  public boolean isValid() {
    return valid;
  }

  public boolean isExternal() {
    return external;
  }

  @Override
  public String toString() {
    return hvEdges + "\n" + ptEdges;
  }

  public static SMGStateInformation of(
      Set<SMGEdgeHasValue> pHves,
      Set<SMGEdgePointsTo> ptes,
      boolean pIsRegionValid,
      boolean pIsRegionExternallyAllocated) {
    return new SMGStateInformation(pHves, ptes, pIsRegionValid, pIsRegionExternallyAllocated);
  }
}
