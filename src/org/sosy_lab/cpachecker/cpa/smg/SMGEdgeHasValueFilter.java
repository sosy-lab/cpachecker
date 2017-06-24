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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SMGEdgeHasValueFilter {

  public static SMGEdgeHasValueFilter objectFilter(SMGObject pObject) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();
    filter.filterByObject(pObject);

    return filter;
  }

  private SMGObject object = null;

  private Integer value = null;
  private boolean valueComplement = false;
  private Integer offset = null;
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

  public SMGEdgeHasValueFilter filterAtOffset(Integer pOffset) {
    offset = pOffset;
    return this;
  }

  public SMGEdgeHasValueFilter filterByType(CType pType) {
    type = pType;
    return this;
  }

  public SMGObject filtersByObject() {
    return object;
  }

  public Integer filtersHavingValue() {
    if (valueComplement) {
      return null;
    } else {
      return value;
    }
  }

  public Integer filtersNotHavingValue() {
    if (valueComplement) {
      return value;
    } else {
      return null;
    }
  }

  public Integer filtersAtOffset() {
    return offset;
  }

  public CType filtersByType() {
    return type;
  }

  public boolean isFilteringByObject() {
    return object != null;
  }

  public boolean isFilteringHavingValue() {
    return value != null && !valueComplement;
  }

  public boolean isFilteringNotHavingValue() {
    return value != null && valueComplement;
  }

  public boolean isFilteringAtOffset() {
    return offset != null;
  }

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

  public Set<SMGEdgeHasValue> filterSet(SMGHasValueEdges pEdges) {
    Set<SMGEdgeHasValue> returnSet = new HashSet<>();
    Set<SMGEdgeHasValue> filtered;
    if (object != null) {
      filtered = pEdges.getEdgesForObject(object);
    } else {
      filtered = pEdges.getHvEdges();
    }
    if (filtered != null) {
      for (SMGEdgeHasValue edge : filtered) {
        if (holdsFor(edge)) {
          returnSet.add(edge);
        }
      }
    }
    return returnSet;
  }

  public Set<SMGEdgeHasValue> filterSet(Set<SMGEdgeHasValue> pEdges) {
    Set<SMGEdgeHasValue> returnSet = new HashSet<>();
    for (SMGEdgeHasValue edge : pEdges) {
      if (holdsFor(edge)) {
        returnSet.add(edge);
      }
    }
    return Collections.unmodifiableSet(returnSet);
  }

  public boolean edgeContainedIn(Set<SMGEdgeHasValue> pEdges) {

    assert value != null;
    assert object != null;
    assert offset != null;
    assert type != null;

    for (SMGEdgeHasValue edge : pEdges) {
      if (holdsFor(edge)) {
        return true;
      }
    }

    return false;
  }

  public Predicate<SMGEdgeHasValue> asPredicate() {
    return new Predicate<SMGEdgeHasValue>() {
      @Override
      public boolean apply(SMGEdgeHasValue pEdge) {
        return SMGEdgeHasValueFilter.this.holdsFor(pEdge);
      }
    };
  }

  public static SMGEdgeHasValueFilter valueFilter(Integer pValue) {

    return new SMGEdgeHasValueFilter().filterHavingValue(pValue);
  }
}