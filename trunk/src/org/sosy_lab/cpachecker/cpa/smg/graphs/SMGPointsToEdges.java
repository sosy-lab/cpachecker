// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/** An immutable collection of points-to-edges. */
public interface SMGPointsToEdges extends Iterable<SMGEdgePointsTo> {

  // Modifying methods

  SMGPointsToEdges addAndCopy(SMGEdgePointsTo pEdge);

  SMGPointsToEdges removeAndCopy(SMGEdgePointsTo pEdge);

  SMGPointsToEdges removeAllEdgesOfObjectAndCopy(SMGObject pObj);

  SMGPointsToEdges removeEdgeWithValueAndCopy(SMGValue pValue);

  // Querying methods

  boolean containsEdgeWithValue(SMGValue pValue);

  /** get an outgoing edge of the {@link SMGValue} if available. */
  @Nullable SMGEdgePointsTo getEdgeWithValue(SMGValue pValue);

  int size();
}
