/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
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

  PredRelation getPathPredicateRelation();

  PredRelation getErrorPredicateRelation();

  PersistentSet<SMGValue> getValues();

  PersistentSet<SMGObject> getObjects();

  SMGHasValueEdges getHVEdges();

  SMGHasValueEdges getHVEdges(SMGEdgeHasValueFilter pFilter);

  Set<SMGEdgePointsTo> getPtEdges(SMGEdgePointsToFilter pFilter);

  SMGPointsToEdges getPTEdges();

  @Nullable
  SMGObject getObjectPointedBy(SMGValue pValue);

  boolean isObjectValid(SMGObject pObject);

  boolean isObjectExternallyAllocated(SMGObject pObject);

  MachineModel getMachineModel();

  TreeMap<Long, Integer> getNullEdgesMapOffsetToSizeForObject(SMGObject pObj);

  boolean isPointer(SMGValue value);

  SMGEdgePointsTo getPointer(SMGValue value);

  boolean isCoveredByNullifiedBlocks(SMGEdgeHasValue pEdge);

  boolean isCoveredByNullifiedBlocks(SMGObject pObject, long pOffset, CType pType);

  boolean haveNeqRelation(SMGValue pV1, SMGValue pV2);

  Set<SMGValue> getNeqsForValue(SMGValue pV);
}