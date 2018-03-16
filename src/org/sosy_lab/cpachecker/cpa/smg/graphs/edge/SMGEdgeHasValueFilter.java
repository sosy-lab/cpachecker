/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

public class SMGEdgeHasValueFilter {

  public static SMGEdgeHasValueFilter objectFilter(SMGObject pObject) {
    return new SMGEdgeHasValueFilter().filterByObject(pObject);
  }

  private SMGObject object = null;
  private Integer value = null;
  private boolean valueComplement = false;
  private Long offset = null;
  private CType type = null;

  public SMGEdgeHasValueFilter filterByObject(SMGObject pObject) {
    object = pObject;
    return this;
  }

  public SMGEdgeHasValueFilter filterHavingValue(Integer pValue) {
    value = pValue;
    valueComplement = false;
    return this;
  }

  public SMGEdgeHasValueFilter filterNotHavingValue(Integer pValue) {
    value = pValue;
    valueComplement = true;
    return this;
  }

  public SMGEdgeHasValueFilter filterAtOffset(long pOffset) {
    offset = pOffset;
    return this;
  }

  public SMGEdgeHasValueFilter filterByType(CType pType) {
    type = pType;
    return this;
  }

  @Deprecated
  public SMGObject filtersByObject() {
    return object;
  }

  @Deprecated
  public Integer filtersHavingValue() {
    if (valueComplement) {
      return null;
    } else {
      return value;
    }
  }

  @Deprecated
  public Integer filtersNotHavingValue() {
    if (valueComplement) {
      return value;
    } else {
      return null;
    }
  }

  @Deprecated
  public Long filtersAtOffset() {
    return offset;
  }

  @Deprecated
  public CType filtersByType() {
    return type;
  }

  @Deprecated
  public boolean isFilteringByObject() {
    return object != null;
  }

  @Deprecated
  public boolean isFilteringHavingValue() {
    return value != null && !valueComplement;
  }

  @Deprecated
  public boolean isFilteringNotHavingValue() {
    return value != null && valueComplement;
  }

  @Deprecated
  public boolean isFilteringAtOffset() {
    return offset != null;
  }

  @Deprecated
  public CType isFilteringAtType() {
    return type;
  }

  public boolean holdsFor(SMGEdgeHasValue pEdge) {
    if (object != null && object != pEdge.getObject()) {
      return false;
    }

    if (value != null) {
      if (valueComplement && pEdge.getValue() == value) {
        return false;
      } else if ( (!valueComplement) && pEdge.getValue() != value) {
        return false;
      }
    }

    if (offset != null && offset != pEdge.getOffset()) {
      return false;
    }

    if (type != null && ! type.getCanonicalType().equals(pEdge.getType().getCanonicalType())) {
      return false;
    }

    return true;
  }

  public Iterable<SMGEdgeHasValue> filter(SMGHasValueEdges pEdges) {
    Set<SMGEdgeHasValue> filtered;
    if (object != null) {
      filtered = pEdges.getEdgesForObject(object);
      if (filtered == null) {
        return ImmutableSet.of();
      }
    } else {
      filtered = pEdges.getHvEdges();
    }
    return filter(filtered);
  }

  /** Info: Please use SMG.getHVEdges(filter) for better performance when filtering for objects. */
  public Iterable<SMGEdgeHasValue> filter(Iterable<SMGEdgeHasValue> pEdges) {
    return Iterables.filter(pEdges, this::holdsFor);
  }

  public boolean edgeContainedIn(Iterable<SMGEdgeHasValue> pEdges) {

    assert value != null;
    assert object != null;
    assert offset != null;
    assert type != null;

    return Iterables.any(pEdges, this::holdsFor);
  }

  public static SMGEdgeHasValueFilter valueFilter(Integer pValue) {
    return new SMGEdgeHasValueFilter().filterHavingValue(pValue);
  }
}