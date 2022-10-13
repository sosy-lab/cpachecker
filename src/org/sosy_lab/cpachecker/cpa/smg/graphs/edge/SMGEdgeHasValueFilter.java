// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgeHasValueFilter {

  protected SMGObject object;
  private SMGValue value;
  private boolean valueComplement;
  private Long offset;
  private long sizeInBits;
  private boolean sizeNotRequired;
  private SMGEdgeHasValue overlapsWith;

  public SMGEdgeHasValueFilter() {
    object = null;
    value = null;
    valueComplement = false;
    offset = null;
    sizeInBits = -1;
    sizeNotRequired = false;
    overlapsWith = null;
  }

  public SMGEdgeHasValueFilter(SMGEdgeHasValueFilter pFilter) {
    object = pFilter.object;
    value = pFilter.value;
    valueComplement = pFilter.valueComplement;
    offset = pFilter.offset;
    sizeInBits = pFilter.sizeInBits;
    sizeNotRequired = pFilter.sizeNotRequired;
    overlapsWith = pFilter.overlapsWith;
  }

  public static class SMGEdgeHasValueFilterByObject extends SMGEdgeHasValueFilter {
    @CanIgnoreReturnValue
    @VisibleForTesting
    @Override
    public SMGEdgeHasValueFilterByObject filterByObject(SMGObject pObject) {
      object = pObject;
      return this;
    }

    @Override
    public SMGHasValueEdges filter(SMGHasValueEdges pEdges) {
      SMGHasValueEdges filtered;
      if (object != null) {
        filtered = pEdges.getEdgesForObject(object);
      } else {
        filtered = pEdges.getHvEdges();
      }
      return filtered;
    }
  }

  public static SMGEdgeHasValueFilterByObject objectFilter(SMGObject pObject) {
    return new SMGEdgeHasValueFilterByObject().filterByObject(pObject);
  }

  public SMGObject getObject() {
    return object;
  }

  public SMGValue getValue() {
    return value;
  }

  public Long getOffset() {
    return offset;
  }

  public long getSize() {
    return sizeInBits;
  }

  public boolean isSizeNotRequired() {
    return sizeNotRequired;
  }

  public SMGEdgeHasValue getOverlapsWith() {
    return overlapsWith;
  }

  @VisibleForTesting
  public SMGEdgeHasValueFilter filterByObject(SMGObject pObject) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.object = pObject;
    return filter;
  }

  public SMGEdgeHasValueFilter filterWithoutSize() {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.sizeNotRequired = true;
    return filter;
  }

  public SMGEdgeHasValueFilter filterHavingValue(SMGValue pValue) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.value = pValue;
    filter.valueComplement = false;
    if (!pValue.isZero()) {
      filter.sizeNotRequired = true;
    }
    return filter;
  }

  public SMGEdgeHasValueFilter filterNotHavingValue(SMGValue pValue) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.value = pValue;
    filter.valueComplement = true;
    if (pValue.isZero()) {
      filter.sizeNotRequired = true;
    }
    return filter;
  }

  public SMGEdgeHasValueFilter filterAtOffset(long pOffset) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.offset = pOffset;
    return filter;
  }

  public SMGEdgeHasValueFilter filterBySize(long pSizeInBits) {
    Preconditions.checkArgument(pSizeInBits >= 0, "negative sizes not allowed for filtering");
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.sizeInBits = pSizeInBits;
    return filter;
  }

  public SMGEdgeHasValueFilter overlapsWith(SMGEdgeHasValue pEdge) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter(this);
    filter.overlapsWith = pEdge;
    return filter;
  }

  public boolean holdsFor(SMGEdgeHasValue pEdge) {
    assert sizeInBits >= 0 || sizeNotRequired || overlapsWith != null;
    if (object != null && object != pEdge.getObject()) {
      return false;
    }

    if (value != null) {
      if (valueComplement && pEdge.getValue().equals(value)) {
        return false;
      } else if (!valueComplement && !pEdge.getValue().equals(value)) {
        return false;
      }
    }

    if (offset != null && offset != pEdge.getOffset()) {
      return false;
    }

    if (overlapsWith != null) {
      if (!overlapsWith.getObject().equals(pEdge.getObject())) {
        return false;
      }
      if (overlapsWith.getOffset() > pEdge.getOffset() + pEdge.getSizeInBits()) {
        return false;
      }
      if (overlapsWith.getOffset() + overlapsWith.getSizeInBits() <= pEdge.getOffset()) {
        return false;
      }
    }

    if (sizeInBits >= 0 && sizeInBits != pEdge.getSizeInBits()) {
      // zero edge with bigger size can hold current edge
      if (sizeInBits < pEdge.getSizeInBits() && pEdge.getValue().isZero()) {
        return true;
      }
      return false;
    }

    return true;
  }

  public Iterable<SMGEdgeHasValue> filter(SMGHasValueEdges pEdges) {
    SMGHasValueEdges filtered;
    if (object != null) {
      filtered = pEdges.getEdgesForObject(object);
    } else {
      filtered = pEdges.getHvEdges();
    }
    return filtered.filter(this);
  }

  public static SMGEdgeHasValueFilter valueFilter(SMGValue pValue) {
    return new SMGEdgeHasValueFilter().filterHavingValue(pValue);
  }

  @Override
  public String toString() {
    return String.format(
        "Filter %s<object=%s@%d, value=%s, size=%d>",
        valueComplement ? "NOT" : "", object, offset, value, sizeInBits);
  }
}
