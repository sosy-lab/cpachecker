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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SMGEdgePointsToFilter {

  private Integer value = null;
  private SMGObject targetObject = null;
  private Integer targetOffset = null;
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

  public SMGEdgePointsToFilter filterAtTargetOffset(Integer pOffset) {
    targetOffset = pOffset;
    return this;
  }

  public SMGEdgePointsToFilter filterByTargetSpecifier(SMGTargetSpecifier pTargetSpecifier) {
    targetSpecifier = pTargetSpecifier;
    return this;
  }

  public SMGObject filtersByTargetObject() {
    return targetObject;
  }

  public Integer filtersHavingValue() {
    return value;
  }

  public Integer filtersAtTargetOffset() {
    return targetOffset;
  }

  public SMGTargetSpecifier filtersByTargetSpecifier() {
    return targetSpecifier;
  }

  public boolean isFilteringByObject() {
    return targetObject != null;
  }

  public boolean isFilteringAtValue() {
    return value != null;
  }

  public boolean isFilteringAtTargetOffset() {
    return targetOffset != null;
  }

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
    if (isFilteringByObject() && !targetObject.equals(pEdge.getObject())) {
      return false;
    }

    if (isFilteringAtValue() && !value.equals(pEdge.getValue())) {
      return false;
    }

    if (isFilteringAtTargetOffset() && !targetOffset.equals(pEdge.getOffset())) {
      return false;
    }

    if (isFilteringByTargetSpecifier() && targetSpecifier != pEdge.getTargetSpecifier()) {
      return false;
    }

    return true;
  }

  public Set<SMGEdgePointsTo> filterSet(Set<SMGEdgePointsTo> pEdges) {
    Set<SMGEdgePointsTo> returnSet = new HashSet<>();
    for (SMGEdgePointsTo edge : pEdges) {
      if (holdsFor(edge)) {
        returnSet.add(edge);
      }
    }
    return Collections.unmodifiableSet(returnSet);
  }
}