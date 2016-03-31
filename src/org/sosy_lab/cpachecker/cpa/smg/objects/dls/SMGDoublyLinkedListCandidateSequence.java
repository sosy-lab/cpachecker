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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinSubSMGsForAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import com.google.common.collect.Iterables;

public class SMGDoublyLinkedListCandidateSequence implements SMGAbstractionCandidate {

  private final SMGDoublyLinkedListCandidate candidate;
  private final int length;

  public SMGDoublyLinkedListCandidateSequence(SMGDoublyLinkedListCandidate pCandidate,
      int pLength) {
    candidate = pCandidate;
    length = pLength;
  }

  public SMGDoublyLinkedListCandidate getCandidate() {
    return candidate;
  }

  public int getLength() {
    return length;
  }

  @Override
  public CLangSMG execute(CLangSMG pSMG) throws SMGInconsistentException {

    SMGObject prevObject = candidate.getObject();
    int nfo = candidate.getNfo();

    for (int i = 0; i < length; i++) {
      SMGEdgeHasValue nextEdge = Iterables.getOnlyElement(pSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(prevObject).filterAtOffset(nfo)));
      SMGObject nextObject = pSMG.getPointer(nextEdge.getValue()).getObject();
      SMGJoinSubSMGsForAbstraction join =
          new SMGJoinSubSMGsForAbstraction(pSMG, prevObject, nextObject, candidate);

      for (SMGObject obj : join.getNonSharedObjectsFromSMG1()) {
        pSMG.removeHeapObjectAndEdges(obj);
      }

      for (SMGObject obj : join.getNonSharedObjectsFromSMG2()) {
        pSMG.removeHeapObjectAndEdges(obj);
      }

      pSMG.removeHeapObjectAndEdges(nextObject);
      pSMG.removeHeapObjectAndEdges(prevObject);
      prevObject = join.getNewAbstractObject();
    }

    return pSMG;
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidateSequence [candidate=" + candidate + ", length=" + length
        + "]";
  }

  @Override
  public int getScore() {
    return getLength();
  }
}