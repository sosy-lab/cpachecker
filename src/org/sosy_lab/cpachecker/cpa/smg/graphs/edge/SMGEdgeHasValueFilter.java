/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgeHasValueFilter {

  public static SMGEdgeHasValueFilter objectFilter(SMGObject pObject) {
    return new SMGEdgeHasValueFilter().filterByObject(pObject);
  }

  private SMGObject object = null;
  private SMGValue value = null;
  private boolean valueComplement = false;
  private Long offset = null;
  private long sizeInBits = -1;

  @VisibleForTesting
  public SMGEdgeHasValueFilter filterByObject(SMGObject pObject) {
    object = pObject;
    return this;
  }

  public SMGEdgeHasValueFilter filterHavingValue(SMGValue pValue) {
    value = pValue;
    valueComplement = false;
    return this;
  }

  public SMGEdgeHasValueFilter filterNotHavingValue(SMGValue pValue) {
    value = pValue;
    valueComplement = true;
    return this;
  }

  public SMGEdgeHasValueFilter filterAtOffset(long pOffset) {
    offset = pOffset;
    return this;
  }

  public SMGEdgeHasValueFilter filterBySize(long pSizeInBits) {
    Preconditions.checkArgument(pSizeInBits >= 0, "negative sizes not allowed for filtering");
    sizeInBits = pSizeInBits;
    return this;
  }

  public boolean holdsFor(SMGEdgeHasValue pEdge) {
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

    if (sizeInBits >= 0 && sizeInBits != pEdge.getSizeInBits()) {
      return false;
    }

    return true;
  }

  public Iterable<SMGEdgeHasValue> filter(SMGHasValueEdges pEdges) {
    Set<SMGEdgeHasValue> filtered;
    if (object != null) {
      filtered = pEdges.getEdgesForObject(object);
    } else {
      filtered = pEdges.getHvEdges();
    }
    return filter(filtered);
  }

  /** Info: Please use SMG.getHVEdges(filter) for better performance when filtering for objects. */
  @VisibleForTesting
  public Iterable<SMGEdgeHasValue> filter(Iterable<SMGEdgeHasValue> pEdges) {
    return Iterables.filter(pEdges, this::holdsFor);
  }

  public static SMGEdgeHasValueFilter valueFilter(SMGValue pValue) {
    return new SMGEdgeHasValueFilter().filterHavingValue(pValue);
  }

  @Override
  public String toString() {
    return String.format(
        "Filter %s<object=%s@%d, value=%s, size=%d>",
        valueComplement ? "" : "NOT", object, offset, value, sizeInBits);
  }
}