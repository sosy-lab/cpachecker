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

  public SMGEdgePointsTo(int pValue, SMGObject pObject, long pOffset) {
    this(
        pValue,
        pObject,
        pOffset,
        pObject instanceof SMGRegion ? SMGTargetSpecifier.REGION : SMGTargetSpecifier.UNKNOWN);
  }

  public SMGEdgePointsTo(int pValue, SMGObject pObject, long pOffset, SMGTargetSpecifier pTg) {
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