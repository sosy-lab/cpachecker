// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs have two kind of edges: {@link SMGEdgeHasValue} and {@link SMGEdgePointsTo}. {@link
 * SMGEdgePointsTo}s lead from addresses ({@link SMGValue}s to {@link SMGObject}s. {@link
 * SMGEdgePointsTo} are labeled by an offset and a {@link SMGTargetSpecifier}. The {@link
 * SMGTargetSpecifier} is only meaningful for abstractions like list segments, where it
 * distinguishes whether a given edge represents the address (or addresses) of the first, last, or
 * each concrete region abstracted by the segment.
 */
public class SMGEdgePointsTo extends SMGEdge {

  private final SMGTargetSpecifier tg;

  /**
   * Create instance.
   *
   * @param pValue the value that points to some object.
   * @param pObject the target object pointed to.
   * @param pOffset the offset relative to the start of the target object, i.e. ZERO represents a
   *     direct pointer, a positive number points into or after the object.
   */
  public SMGEdgePointsTo(SMGValue pValue, SMGObject pObject, long pOffset) {
    this(
        pValue,
        pObject,
        pOffset,
        pObject instanceof SMGRegion ? SMGTargetSpecifier.REGION : SMGTargetSpecifier.UNKNOWN);
  }

  public SMGEdgePointsTo(SMGValue pValue, SMGObject pObject, long pOffset, SMGTargetSpecifier pTg) {
    super(pValue, pObject, pOffset);
    tg = pTg;
  }

  @Override
  public String toString() {
    return value + "->" + object.getLabel() + "+" + getOffset() + 'b';
  }

  public SMGTargetSpecifier getTargetSpecifier() {
    return tg;
  }

  @Override
  public boolean isConsistentWith(SMGEdge edge) {
    /*
     * different value- > different place
     * same value -> same place
     */
    if (!(edge instanceof SMGEdgePointsTo)) {
      return false;
    }

    final SMGEdgePointsTo other = (SMGEdgePointsTo) edge;
    if (value == other.value) {
      return getOffset() == other.getOffset() && object == other.object && tg == other.tg;
    } else {
      return getOffset() != other.getOffset()
          || object != other.object
          || (tg != SMGTargetSpecifier.UNKNOWN
              && other.tg != SMGTargetSpecifier.UNKNOWN
              && tg != other.tg);
    }
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + tg.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SMGEdgePointsTo)) {
      return false;
    }
    SMGEdgePointsTo other = (SMGEdgePointsTo) obj;
    return super.equals(obj) && tg == other.tg;
  }
}
