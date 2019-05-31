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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
  private CType type = null;

  public SMGObject getObject() {
    return object;
  }

  public SMGValue getValue() {
    return value;
  }

  public Long getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

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

  public SMGEdgeHasValueFilter filterByType(CType pType) {
    type = pType;
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

    if (type != null && ! type.getCanonicalType().equals(pEdge.getType().getCanonicalType())) {
      return false;
    }

    return true;
  }

  public SMGHasValueEdges filter(SMGHasValueEdges pEdges) {
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
        "Filter <object=%s@%d, value%s=%s, type=%s>",
        object, offset, valueComplement ? "" : "!", value, type);
  }
}