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

import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** An immutable collection of has-value-edges.
 * Should keep invariant: All has-value-edges, corresponding to one object, shouldn't interleave.
 * Also it is possible to provide implementation with sorting by objects and offsets */
public interface SMGHasValueEdges extends Iterable<SMGEdgeHasValue> {

  // Modifying methods

  SMGHasValueEdges removeAllEdgesOfObjectAndCopy(SMGObject pObj);

  SMGHasValueEdges addEdgeAndCopy(SMGEdgeHasValue pEdge);

  SMGHasValueEdges removeEdgeAndCopy(SMGEdgeHasValue pEdge);

  // Querying methods

  /** get all outgoing edges of all {@link SMGObject}s. */
  SMGHasValueEdges getHvEdges();

  SMGHasValueEdges filter(SMGEdgeHasValueFilter pFilter);

  /** get all outgoing edges of an {@link SMGObject}, e.g., all values of this object. */
  SMGHasValueEdges getEdgesForObject(SMGObject pObject);

  int size();

  boolean contains(SMGEdgeHasValue pHv);

  Iterable<SMGEdgeHasValue> getOverlapping(SMGEdgeHasValue pNew_edge);
}