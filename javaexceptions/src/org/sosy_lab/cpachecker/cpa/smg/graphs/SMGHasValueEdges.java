// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/**
 * An immutable collection of has-value-edges. Should keep invariant: All has-value-edges,
 * corresponding to one object, shouldn't interleave. Also it is possible to provide implementation
 * with sorting by objects and offsets
 */
public interface SMGHasValueEdges extends Iterable<SMGEdgeHasValue> {

  // Modifying methods

  SMGHasValueEdges removeAllEdgesOfObjectAndCopy(SMGObject pObj);

  SMGHasValueEdges addEdgeAndCopy(SMGEdgeHasValue pEdge);

  SMGHasValueEdges removeEdgeAndCopy(SMGEdgeHasValue pEdge);

  // Querying methods

  /** get all outgoing edges of all {@link SMGObject}s. */
  SMGHasValueEdges getHvEdges();

  Iterable<SMGEdgeHasValue> filter(SMGEdgeHasValueFilter pFilter);

  /** get all outgoing edges of an {@link SMGObject}, e.g., all values of this object. */
  SMGHasValueEdges getEdgesForObject(SMGObject pObject);

  int size();

  boolean contains(SMGEdgeHasValue pHv);

  SMGHasValueEdges addEdgesForObject(Iterable<SMGEdgeHasValue> pEdgesSet);

  boolean isEmpty();
}
