// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** An immutable collection of has-value-edges. */
public interface SMGHasValueEdges {

  // Modifying methods

  SMGHasValueEdges removeAllEdgesOfObjectAndCopy(SMGObject pObj);

  SMGHasValueEdges addEdgeAndCopy(SMGEdgeHasValue pEdge);

  SMGHasValueEdges removeEdgeAndCopy(SMGEdgeHasValue pEdge);

  // Querying methods

  /** get all outgoing edges of all {@link SMGObject}s. */
  ImmutableSet<SMGEdgeHasValue> getHvEdges();

  /** get all outgoing edges of an {@link SMGObject}, e.g., all values of this object. */
  ImmutableSet<SMGEdgeHasValue> getEdgesForObject(SMGObject pObject);
}