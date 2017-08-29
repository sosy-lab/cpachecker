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

import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.Pair;

public abstract class SMGJoinAbstractListProgress<K, C extends SMGListCandidate<?>> {

  protected final Map<SMGObject, Map<K, C>> candidates = new HashMap<>();
  protected final Map<Pair<C, SMGJoinStatus>, Integer> candidateLength = new HashMap<>();

  public SMGJoinAbstractListProgress() {}

  /** @param pHasToBeLastInSequence used in some sub-class */
  public void updateProgress(
      C pPrevCandidate, C pCandidate, SMGJoinStatus pStatus, boolean pHasToBeLastInSequence) {

    switch (pStatus) {
      case EQUAL:
        updateEqualSegment(pCandidate, pPrevCandidate);
        break;
      case RIGHT_ENTAIL:
        updateREntailSegment(pCandidate, pPrevCandidate);
        break;
      case LEFT_ENTAIL:
        updateLEntailSegment(pCandidate, pPrevCandidate);
        break;
      case INCOMPARABLE:
        updateIncomparableSegment(pCandidate, pPrevCandidate);
        break;
      default:
        throw new AssertionError();
    }
  }

  private void updateIncomparableSegment(C pCandidate, C pPrevCandidate) {
    Pair<C, SMGJoinStatus> equalLengthKey = Pair.of(pCandidate, SMGJoinStatus.EQUAL);
    Pair<C, SMGJoinStatus> lELengthKey = Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
    Pair<C, SMGJoinStatus> rELengthKey = Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
    Pair<C, SMGJoinStatus> incLengthKey = Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE);

    if (candidateLength.containsKey(equalLengthKey)
        || candidateLength.containsKey(lELengthKey)
        || candidateLength.containsKey(rELengthKey)
        || candidateLength.containsKey(incLengthKey)) {
      int length1 = getMaxLength(equalLengthKey, lELengthKey);
      int length2 = getMaxLength(rELengthKey, incLengthKey);
      int length = Math.max(length1, length2);
      candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE), length + 1);
    }
  }

  private void updateLEntailSegment(C pCandidate, C pPrevCandidate) {
    Pair<C, SMGJoinStatus> equalLengthKey = Pair.of(pCandidate, SMGJoinStatus.EQUAL);
    Pair<C, SMGJoinStatus> lELengthKey = Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
    Pair<C, SMGJoinStatus> rELengthKey = Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
    Pair<C, SMGJoinStatus> incLengthKey = Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE);

    if (candidateLength.containsKey(equalLengthKey) || candidateLength.containsKey(lELengthKey)) {
      int length = getMaxLength(equalLengthKey, lELengthKey);
      candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.LEFT_ENTAIL), length + 1);
    }

    if (candidateLength.containsKey(rELengthKey) || candidateLength.containsKey(incLengthKey)) {
      int length = getMaxLength(rELengthKey, incLengthKey);
      candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE), length + 1);
    }
  }

  private void updateREntailSegment(C pCandidate, C pPrevCandidate) {

    Pair<C, SMGJoinStatus> equalLengthKey = Pair.of(pCandidate, SMGJoinStatus.EQUAL);
    Pair<C, SMGJoinStatus> lELengthKey = Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
    Pair<C, SMGJoinStatus> rELengthKey = Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
    Pair<C, SMGJoinStatus> incLengthKey = Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE);

    if (candidateLength.containsKey(equalLengthKey) || candidateLength.containsKey(rELengthKey)) {
      int length = getMaxLength(equalLengthKey, rELengthKey);
      candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.RIGHT_ENTAIL), length + 1);
    }

    if (candidateLength.containsKey(lELengthKey) || candidateLength.containsKey(incLengthKey)) {
      int length = getMaxLength(lELengthKey, incLengthKey);
      candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE), length + 1);
    }
  }

  private int getMaxLength(Pair<C, SMGJoinStatus> pKey1, Pair<C, SMGJoinStatus> pKey2) {

    int length1 = candidateLength.containsKey(pKey1) ? candidateLength.get(pKey1) : 0;
    int length2 = candidateLength.containsKey(pKey2) ? candidateLength.get(pKey2) : 0;

    return Math.max(length1, length2);
  }

  private void updateEqualSegment(C pCandidate, C pPrevCandidate) {

    if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.EQUAL))) {
      candidateLength.put(
          Pair.of(pPrevCandidate, SMGJoinStatus.EQUAL),
          candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.EQUAL)) + 1);
    }

    if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL))) {
      candidateLength.put(
          Pair.of(pPrevCandidate, SMGJoinStatus.LEFT_ENTAIL),
          candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL)) + 1);
    }

    if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL))) {
      candidateLength.put(
          Pair.of(pPrevCandidate, SMGJoinStatus.RIGHT_ENTAIL),
          candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL)) + 1);
    }

    if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE))) {
      candidateLength.put(
          Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE),
          candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE)) + 1);
    }
  }

  public C getCandidate(SMGObject pObject, K key) {
    return candidates.get(pObject).get(key);
  }

  public void initializeLastInSequenceCandidate(C pCandidate) {
    candidateLength.put(Pair.of(pCandidate, SMGJoinStatus.EQUAL), 1);
  }

  public boolean containsCandidate(SMGObject pObject, K key) {
    return candidates.containsKey(pObject) && candidates.get(pObject).containsKey(key);
  }

  public void putCandidiateMap(SMGObject pObject) {
    assert !candidates.containsKey(pObject);
    candidates.put(pObject, new HashMap<>());
  }

  public boolean containsCandidateMap(SMGObject pObject) {
    return candidates.containsKey(pObject);
  }

  public Set<SMGAbstractionCandidate> getValidCandidates(
      int pSeqLengthSubGraphEqualityThreshold,
      int pSeqLengthSubGraphEntailmentThreshold,
      int pSeqLengthSubGraphIncomparabilityThreshold,
      CLangSMG pSMG,
      Set<? extends SMGAbstractListCandidateSequenceBlock<?>> pAbstractListBlocks) {

    Set<SMGAbstractionCandidate> resultBeforeBlocks = new HashSet<>();

    for (Map<K, C> objCandidates : candidates.values()) {
      for (C candidate : objCandidates.values()) {
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
    Integer length = candidateLength.get(Pair.of(pCandidate, joinStatus));
    if (length != null && length >= pThreshold) {
      pResultBeforeBlocks.add(getCandidat(pCandidate, length, joinStatus, pSMG));
    }
  }

  protected abstract SMGAbstractionCandidate getCandidat(
      C pCandidate, int length, SMGJoinStatus status, CLangSMG pSMG);
}
