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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGJoinAbstractListProgress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.Pair;

class SMGJoinDllProgress
    extends SMGJoinAbstractListProgress<Pair<Long, Long>, SMGDoublyLinkedListCandidate> {

  public SMGJoinDllProgress() {}

  @Override
  public void updateProgress(
      SMGDoublyLinkedListCandidate pPrevCandidate,
      SMGDoublyLinkedListCandidate pCandidate,
      SMGJoinStatus pStatus,
      boolean pHasToBeLastInSequence) {

    if (pHasToBeLastInSequence) {
      /*Due to the nature of the algorithm, this is always the second segment to be joined from the start*/
      assert candidateLength.get(pPrevCandidate, SMGJoinStatus.EQUAL) == 1;
      candidateLength.put(pPrevCandidate, pStatus, 2);
      return;
    }

    super.updateProgress(pPrevCandidate, pCandidate, pStatus, pHasToBeLastInSequence);
  }

  public void initializeCandidiate(SMGDoublyLinkedListCandidate pCandidate) {
    candidates.put(
        pCandidate.getStartObject(),
        Pair.of(pCandidate.getShape().getNfo(), pCandidate.getShape().getPfo()),
        pCandidate);
    candidateLength.put(pCandidate, SMGJoinStatus.EQUAL, 1);
  }

  @Override
  protected SMGDoublyLinkedListCandidateSequence getCandidat(
      SMGDoublyLinkedListCandidate pCandidate, int length, SMGJoinStatus status, CLangSMG pSMG) {
    return new SMGDoublyLinkedListCandidateSequence(pCandidate, length, status, isDllPartOfSequence(pCandidate, length, pSMG));
  }

  private boolean isDllPartOfSequence(
      SMGDoublyLinkedListCandidate pCandidate, int pLength, CLangSMG pSMG) {

    SMGObject nextObject = pCandidate.getStartObject();
    if (nextObject.getKind() == SMGObjectKind.DLL) {
      return true;
    }

    for (int i = 1; i < pLength; i++) {
      SMGEdgeHasValue nextHveEdge = Iterables.getOnlyElement(pSMG.getHVEdges(
          SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(pCandidate.getShape().getNfo())));
      nextObject = pSMG.getPointer(nextHveEdge.getValue()).getObject();
      if (nextObject.getKind() == SMGObjectKind.DLL) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return "SMGJoinDllProgress [candidates=" + candidates + ", candidateLength=" + candidateLength + "]";
  }
}