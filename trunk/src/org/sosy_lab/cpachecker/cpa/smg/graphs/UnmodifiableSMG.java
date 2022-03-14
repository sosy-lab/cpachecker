// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

/**
 * A view on a {@link SMG}, where no modifications are allowed.
 *
 * <p>All returned Collections are unmodifiable.
 */
public interface UnmodifiableSMG {

  /**
   * Returns mutable instance of subclass. Changes to the returned instance are independent of this
   * immutable instance and do not change it.
   */
  SMG copyOf();

  SMGPredicateRelation getPathPredicateRelation();

  SMGPredicateRelation getErrorPredicateRelation();

  PersistentSet<SMGValue> getValues();

  PersistentSet<SMGObject> getObjects();

  SMGHasValueEdges getHVEdges();

  Iterable<SMGEdgeHasValue> getHVEdges(SMGEdgeHasValueFilter pFilter);

  SMGHasValueEdges getHVEdges(SMGEdgeHasValueFilterByObject pFilter);

  Set<SMGEdgePointsTo> getPtEdges(SMGEdgePointsToFilter pFilter);

  SMGPointsToEdges getPTEdges();

  @Nullable SMGObject getObjectPointedBy(SMGValue pValue);

  boolean isObjectValid(SMGObject pObject);

  boolean isObjectExternallyAllocated(SMGObject pObject);

  MachineModel getMachineModel();

  int getSizeofPtrInBits();

  //  FIXME: replace by filter
  TreeMap<Long, Long> getNullEdgesMapOffsetToSizeForObject(SMGObject pObj);

  boolean isPointer(SMGValue value);

  SMGEdgePointsTo getPointer(SMGValue value);

  boolean isCoveredByNullifiedBlocks(SMGEdgeHasValue pEdge);

  boolean haveNeqRelation(SMGValue pV1, SMGValue pV2);

  Set<SMGValue> getNeqsForValue(SMGValue pV);

  PersistentSet<SMGObject> getValidObjects();
}
