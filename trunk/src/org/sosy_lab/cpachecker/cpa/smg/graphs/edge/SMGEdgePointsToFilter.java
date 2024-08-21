// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPointsToEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgePointsToFilter {

  /** the target of the CMGEdgePointsTo for filtering. */
  private final SMGObject targetObject;

  private SMGValue value = null;
  private Long targetOffset = null;
  private SMGTargetSpecifier targetSpecifier = null;

  private SMGEdgePointsToFilter(SMGObject pTargetObject) {
    targetObject = Preconditions.checkNotNull(pTargetObject, "fitlering for NULL might be useless");
  }

  @CanIgnoreReturnValue
  public SMGEdgePointsToFilter filterHavingValue(SMGValue pValue) {
    value = pValue;
    return this;
  }

  @CanIgnoreReturnValue
  public SMGEdgePointsToFilter filterAtTargetOffset(Long pOffset) {
    targetOffset = pOffset;
    return this;
  }

  @CanIgnoreReturnValue
  public SMGEdgePointsToFilter filterByTargetSpecifier(SMGTargetSpecifier pTargetSpecifier) {
    targetSpecifier = pTargetSpecifier;
    return this;
  }

  public static SMGEdgePointsToFilter targetObjectFilter(SMGObject pTargetObject) {
    return new SMGEdgePointsToFilter(pTargetObject);
  }

  public boolean holdsFor(SMGEdgePointsTo pEdge) {
    if (targetObject != null && !targetObject.equals(pEdge.getObject())) {
      return false;
    }

    if (value != null && !value.equals(pEdge.getValue())) {
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

    if (value != null) {
      SMGEdgePointsTo result = edges.getEdgeWithValue(value);
      if (result == null) {
        return ImmutableSet.of();
      } else {
        return ImmutableSet.of(result);
      }
    }

    return Iterables.filter(edges, this::holdsFor);
  }
}
