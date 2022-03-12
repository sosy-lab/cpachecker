// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
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

    pPrevCandidate.updateLastObject(pCandidate.getLastObject());

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
      SMGDoublyLinkedListCandidate pCandidate,
      int length,
      SMGJoinStatus status,
      UnmodifiableCLangSMG pSMG) {
    return new SMGDoublyLinkedListCandidateSequence(
        pCandidate, length, status, isDllPartOfSequence(pCandidate, length, pSMG));
  }

  private boolean isDllPartOfSequence(
      SMGDoublyLinkedListCandidate pCandidate, int pLength, UnmodifiableCLangSMG pSMG) {

    SMGObject nextObject = pCandidate.getStartObject();
    if (nextObject.getKind() == SMGObjectKind.DLL) {
      return true;
    }

    for (int i = 1; i < pLength; i++) {
      SMGEdgeHasValue nextHveEdge =
          Iterables.getOnlyElement(
              pSMG.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(nextObject)
                      .filterAtOffset(pCandidate.getShape().getNfo())
                      .filterBySize(pSMG.getSizeofPtrInBits())));
      nextObject = pSMG.getPointer(nextHveEdge.getValue()).getObject();
      if (nextObject.getKind() == SMGObjectKind.DLL) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return "SMGJoinDllProgress [candidates="
        + candidates
        + ", candidateLength="
        + candidateLength
        + "]";
  }
}
