// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGJoinAbstractListProgress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;

class SMGJoinSllProgress extends SMGJoinAbstractListProgress<Long, SMGSingleLinkedListCandidate> {

  public SMGJoinSllProgress() {}

  public void initializeCandidiate(SMGSingleLinkedListCandidate pCandidate) {
    candidates.put(pCandidate.getStartObject(), pCandidate.getShape().getNfo(), pCandidate);
    candidateLength.put(pCandidate, SMGJoinStatus.EQUAL, 1);
  }

  @Override
  protected SMGSingleLinkedListCandidateSequence getCandidat(
      SMGSingleLinkedListCandidate pCandidate,
      int length,
      SMGJoinStatus status,
      UnmodifiableCLangSMG pSMG) {
    return new SMGSingleLinkedListCandidateSequence(
        pCandidate, length, status, isPartOfSllSequence(pCandidate, length, pSMG));
  }

  private boolean isPartOfSllSequence(
      SMGSingleLinkedListCandidate pCandidate, int pLength, UnmodifiableCLangSMG pSmg) {

    SMGObject nextObject = pCandidate.getStartObject();
    if (nextObject.getKind() == SMGObjectKind.SLL) {
      return true;
    }

    for (int i = 1; i < pLength; i++) {
      SMGEdgeHasValue nextHveEdge =
          Iterables.getOnlyElement(
              pSmg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(nextObject)
                      .filterAtOffset(pCandidate.getShape().getNfo())
                      .filterBySize(pSmg.getSizeofPtrInBits())));
      nextObject = pSmg.getPointer(nextHveEdge.getValue()).getObject();
      if (nextObject.getKind() == SMGObjectKind.SLL) {
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
