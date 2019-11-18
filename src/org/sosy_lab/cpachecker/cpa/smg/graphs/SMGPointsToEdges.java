/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
  @Nullable
  SMGEdgePointsTo getEdgeWithValue(SMGValue pValue);

  int size();
}
