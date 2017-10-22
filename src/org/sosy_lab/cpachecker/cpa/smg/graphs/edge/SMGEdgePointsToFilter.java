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
package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPointsToEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

public class SMGEdgePointsToFilter {

  private Integer value = null;
  private SMGObject targetObject = null;
  private Long targetOffset = null;
  private SMGTargetSpecifier targetSpecifier = null;

  private SMGEdgePointsToFilter() {}

  public SMGEdgePointsToFilter filterByTargetObject(SMGObject pObject) {
    targetObject = pObject;
    return this;
  }

  public SMGEdgePointsToFilter filterHavingValue(Integer pValue) {
    value = pValue;
    return this;
  }

  public SMGEdgePointsToFilter filterAtTargetOffset(Long pOffset) {
    targetOffset = pOffset;
    return this;
  }

  public SMGEdgePointsToFilter filterByTargetSpecifier(SMGTargetSpecifier pTargetSpecifier) {
    targetSpecifier = pTargetSpecifier;
    return this;
  }

  @Deprecated
  public SMGObject filtersByTargetObject() {
    return targetObject;
  }

  public Integer filtersHavingValue() {
    return value;
  }

  @Deprecated
  public Long filtersAtTargetOffset() {
    return targetOffset;
  }

  @Deprecated
  public SMGTargetSpecifier filtersByTargetSpecifier() {
    return targetSpecifier;
  }

  @Deprecated
  public boolean isFilteringByObject() {
    return targetObject != null;
  }

  public boolean isFilteringAtValue() {
    return value != null;
  }

  @Deprecated
  public boolean isFilteringAtTargetOffset() {
    return targetOffset != null;
  }

  @Deprecated
  public boolean isFilteringByTargetSpecifier() {
    return targetSpecifier != null;
  }

  public static SMGEdgePointsToFilter valueFilter(Integer pValue) {
    return new SMGEdgePointsToFilter().filterHavingValue(pValue);
  }

  public static SMGEdgePointsToFilter targetObjectFilter(SMGObject pTargetObject) {
    return new SMGEdgePointsToFilter().filterByTargetObject(pTargetObject);
  }

  public boolean holdsFor(SMGEdgePointsTo pEdge) {
    if (targetObject != null && !targetObject.equals(pEdge.getObject())) {
      return false;
    }

    if (isFilteringAtValue() && !value.equals(pEdge.getValue())) {
      return false;
    }

    if (targetOffset != null && !targetOffset.equals(pEdge.getOffset())) {
      return false;
    }

    if (targetSpecifier != null && targetSpecifier != pEdge.getTargetSpecifier()) {
      return false;
    }

    return true;
  }

  public Iterable<SMGEdgePointsTo> filter(SMGPointsToEdges edges) {

    if (isFilteringAtValue()) {
      SMGEdgePointsTo result = edges.getEdgeWithValue(filtersHavingValue());
      if (result == null) {
        return ImmutableSet.of();
      } else {
        return ImmutableSet.of(result);
      }
    }

    return filter((Iterable<SMGEdgePointsTo>)edges);
  }

  public Iterable<SMGEdgePointsTo> filter(Iterable<SMGEdgePointsTo> edges) {
    return Iterables.filter(edges, this::holdsFor);
  }
}