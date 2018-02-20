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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;

public abstract class SMGJoinAbstractListProgress<K, C extends SMGListCandidate<?>> {

  protected final Table<SMGObject, K, C> candidates = HashBasedTable.create();
  protected final Table<C, SMGJoinStatus, Integer> candidateLength = HashBasedTable.create();

  public SMGJoinAbstractListProgress() {}

  /** @param pHasToBeLastInSequence used in some sub-class */
  public void updateProgress(
      C pPrevCandidate, C pCandidate, SMGJoinStatus pStatus, boolean pHasToBeLastInSequence) {

    Map<SMGJoinStatus, Integer> candidateLengths = candidateLength.row(pCandidate);

    switch (pStatus) {
      case EQUAL:
        updateEqualSegment(candidateLengths, pPrevCandidate);
        break;
      case RIGHT_ENTAIL:
        updateREntailSegment(candidateLengths, pPrevCandidate);
        break;
      case LEFT_ENTAIL:
        updateLEntailSegment(candidateLengths, pPrevCandidate);
        break;
      case INCOMPARABLE:
        updateIncomparableSegment(candidateLengths, pPrevCandidate);
        break;
      default:
        throw new AssertionError();
    }
  }

  private void updateIncomparableSegment(
      Map<SMGJoinStatus, Integer> candidateLengths, C pPrevCandidate) {

    if (candidateLengths.containsKey(SMGJoinStatus.EQUAL)
        || candidateLengths.containsKey(SMGJoinStatus.LEFT_ENTAIL)
        || candidateLengths.containsKey(SMGJoinStatus.RIGHT_ENTAIL)
        || candidateLengths.containsKey(SMGJoinStatus.INCOMPARABLE)) {
      int length =
          getMaxLength(
              candidateLengths,
              SMGJoinStatus.EQUAL,
              SMGJoinStatus.LEFT_ENTAIL,
              SMGJoinStatus.RIGHT_ENTAIL,
              SMGJoinStatus.INCOMPARABLE);
      candidateLength.put(pPrevCandidate, SMGJoinStatus.INCOMPARABLE, length + 1);
    }
  }

  private void updateLEntailSegment(
      Map<SMGJoinStatus, Integer> candidateLengths, C pPrevCandidate) {

    if (candidateLengths.containsKey(SMGJoinStatus.EQUAL)
        || candidateLengths.containsKey(SMGJoinStatus.LEFT_ENTAIL)) {
      int length = getMaxLength(candidateLengths, SMGJoinStatus.EQUAL, SMGJoinStatus.LEFT_ENTAIL);
      candidateLength.put(pPrevCandidate, SMGJoinStatus.LEFT_ENTAIL, length + 1);
    }

    if (candidateLengths.containsKey(SMGJoinStatus.RIGHT_ENTAIL)
        || candidateLengths.containsKey(SMGJoinStatus.INCOMPARABLE)) {
      int length =
          getMaxLength(candidateLengths, SMGJoinStatus.RIGHT_ENTAIL, SMGJoinStatus.INCOMPARABLE);
      candidateLength.put(pPrevCandidate, SMGJoinStatus.INCOMPARABLE, length + 1);
    }
  }

  private void updateREntailSegment(
      Map<SMGJoinStatus, Integer> candidateLengths, C pPrevCandidate) {

    if (candidateLengths.containsKey(SMGJoinStatus.EQUAL)
        || candidateLengths.containsKey(SMGJoinStatus.RIGHT_ENTAIL)) {
      int length = getMaxLength(candidateLengths, SMGJoinStatus.EQUAL, SMGJoinStatus.RIGHT_ENTAIL);
      candidateLength.put(pPrevCandidate, SMGJoinStatus.RIGHT_ENTAIL, length + 1);
    }

    if (candidateLengths.containsKey(SMGJoinStatus.LEFT_ENTAIL)
        || candidateLengths.containsKey(SMGJoinStatus.INCOMPARABLE)) {
      int length =
          getMaxLength(candidateLengths, SMGJoinStatus.LEFT_ENTAIL, SMGJoinStatus.INCOMPARABLE);
      candidateLength.put(pPrevCandidate, SMGJoinStatus.INCOMPARABLE, length + 1);
    }
  }

  /** returns the maximum of available mappings or zero. */
  private int getMaxLength(Map<SMGJoinStatus, Integer> candidateLengths, SMGJoinStatus... status) {
    int result = 0;
    for (SMGJoinStatus s : status) {
      result = Math.max(result, candidateLengths.getOrDefault(s, 0));
    }
    return result;
  }

  private void updateEqualSegment(Map<SMGJoinStatus, Integer> candidateLengths, C pPrevCandidate) {

    if (candidateLengths.containsKey(SMGJoinStatus.EQUAL)) {
      candidateLength.put(
          pPrevCandidate, SMGJoinStatus.EQUAL, candidateLengths.get(SMGJoinStatus.EQUAL) + 1);
    }

    if (candidateLengths.containsKey(SMGJoinStatus.LEFT_ENTAIL)) {
      candidateLength.put(
          pPrevCandidate,
          SMGJoinStatus.LEFT_ENTAIL,
          candidateLengths.get(SMGJoinStatus.LEFT_ENTAIL) + 1);
    }

    if (candidateLengths.containsKey(SMGJoinStatus.RIGHT_ENTAIL)) {
      candidateLength.put(
          pPrevCandidate,
          SMGJoinStatus.RIGHT_ENTAIL,
          candidateLengths.get(SMGJoinStatus.RIGHT_ENTAIL) + 1);
    }

    if (candidateLengths.containsKey(SMGJoinStatus.INCOMPARABLE)) {
      candidateLength.put(
          pPrevCandidate,
          SMGJoinStatus.INCOMPARABLE,
          candidateLengths.get(SMGJoinStatus.INCOMPARABLE) + 1);
    }
  }

  public @Nullable C getCandidate(SMGObject pObject, K key) {
    return candidates.get(pObject, key);
  }

  public void initializeLastInSequenceCandidate(C pCandidate) {
    candidateLength.put(pCandidate, SMGJoinStatus.EQUAL, 1);
  }

  public boolean containsCandidate(SMGObject pObject, K key) {
    return candidates.contains(pObject, key);
  }

  public boolean containsCandidateMap(SMGObject pObject) {
    return candidates.containsRow(pObject);
  }

  public Set<SMGAbstractionCandidate> getValidCandidates(
      int pSeqLengthSubGraphEqualityThreshold,
      int pSeqLengthSubGraphEntailmentThreshold,
      int pSeqLengthSubGraphIncomparabilityThreshold,
      CLangSMG pSMG,
      Set<? extends SMGAbstractListCandidateSequenceBlock<?>> pAbstractListBlocks) {

    Set<SMGAbstractionCandidate> resultBeforeBlocks = new HashSet<>();

    for (C candidate : candidates.values()) {
      addCandidate(
          SMGJoinStatus.EQUAL,
          candidate,
          pSeqLengthSubGraphEqualityThreshold,
          pSMG,
          resultBeforeBlocks);
      addCandidate(
          SMGJoinStatus.LEFT_ENTAIL,
          candidate,
          pSeqLengthSubGraphEntailmentThreshold,
          pSMG,
          resultBeforeBlocks);
      addCandidate(
          SMGJoinStatus.RIGHT_ENTAIL,
          candidate,
          pSeqLengthSubGraphEntailmentThreshold,
          pSMG,
          resultBeforeBlocks);
      addCandidate(
          SMGJoinStatus.INCOMPARABLE,
          candidate,
          pSeqLengthSubGraphIncomparabilityThreshold,
          pSMG,
          resultBeforeBlocks);
    }

    Set<SMGAbstractionCandidate> result = new HashSet<>();
    for (SMGAbstractionCandidate candidate : resultBeforeBlocks) {
      if (Iterables.all(pAbstractListBlocks, b -> !b.isBlocked(candidate, pSMG))) {
        result.add(candidate);
      }
    }
    return result;
  }

  private void addCandidate(
      SMGJoinStatus joinStatus,
      C pCandidate,
      int pThreshold,
      CLangSMG pSMG,
      Set<SMGAbstractionCandidate> pResultBeforeBlocks) {
    Integer length = candidateLength.get(pCandidate, joinStatus);
    if (length != null && length >= pThreshold) {
      pResultBeforeBlocks.add(getCandidat(pCandidate, length, joinStatus, pSMG));
    }
  }

  protected abstract SMGAbstractionCandidate getCandidat(
      C pCandidate, int length, SMGJoinStatus status, CLangSMG pSMG);
}
