/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

public class SMGSingleLinkedListCandidate implements SMGAbstractionCandidate {
  private final SMGObject start;
  private final int offset;
  private int length;

  public SMGSingleLinkedListCandidate(SMGObject pStart, int pOffset, int pLength) {
    start = pStart;
    offset = pOffset;
    length = pLength;
  }

  @Override
  public int getScore() {
    return 0;
  }

  @Override
  public CLangSMG execute(CLangSMG pSMG) {
    CLangSMG newSMG = new CLangSMG(pSMG);
    SMGSingleLinkedList sll = new SMGSingleLinkedList((SMGRegion)start, offset, length);
    newSMG.addHeapObject(sll);

    //TODO: Better filtering of the pointers!!!
    for (SMGEdgePointsTo pt : newSMG.getPTEdges().values()) {
      if (pt.getObject().equals(start)) {
        SMGEdgePointsTo newPt = new SMGEdgePointsTo(pt.getValue(), sll, pt.getOffset());
        newSMG.removePointsToEdge(pt.getValue());
        newSMG.addPointsToEdge(newPt);
      }
    }

    SMGObject node = start;
    Integer value = null;
    SMGEdgeHasValue edgeToFollow = null;
    for (int i = 0; i < length; i++) {
      if (value != null) {
        newSMG.removeValue(value);
        newSMG.removePointsToEdge(value);
      }

      Set<SMGEdgeHasValue> outboundEdges = newSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(node).filterAtOffset(offset));
      edgeToFollow = null;
      for (SMGEdgeHasValue outbound : outboundEdges) {
        CType fieldType = outbound.getType();
        if (fieldType instanceof CPointerType) {
          edgeToFollow = outbound;
          break;
        }
      }
      if (edgeToFollow == null) {
        edgeToFollow = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, offset, node, newSMG.getNullValue());
      }

      value = edgeToFollow.getValue();
      newSMG.removeHeapObjectAndEdges(node);
      node = newSMG.getPointer(value).getObject();
    }
    SMGEdgeHasValue newOutbound = new SMGEdgeHasValue(edgeToFollow.getType(), offset, sll, value);
    newSMG.addHasValueEdge(newOutbound);

    return newSMG;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  public void addLength(int pLength) {
    length += pLength;
  }

  public boolean isCompatibleWith(SMGSingleLinkedListCandidate pOther) {
    return (offset == pOther.offset) && (start.getSize() == pOther.start.getSize());
  }

  public SMGObject getStart() {
    return start;
  }

  @Override
  public String toString() {
    return "SLL CANDIDATE(start=" + start + ", offset=" + offset + ", length=" + length + ")";
  }
}