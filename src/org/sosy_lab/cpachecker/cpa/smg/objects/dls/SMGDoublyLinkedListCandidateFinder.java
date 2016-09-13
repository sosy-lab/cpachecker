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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionFinder;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinSubSMGsForAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SMGDoublyLinkedListCandidateFinder implements SMGAbstractionFinder {

  private final int seqLengthSubGraphEqualityThreshold;
  private final int seqLengthSubGraphEntailmentThreshold;
  private final int seqLengthSubGraphIncomparabilityThreshold;

  public SMGDoublyLinkedListCandidateFinder() {
    seqLengthSubGraphEqualityThreshold = 2;
    seqLengthSubGraphEntailmentThreshold = 2;
    seqLengthSubGraphIncomparabilityThreshold = 3;
  }

  public SMGDoublyLinkedListCandidateFinder(int pSeqLengthSubGraphEqualityThreshold,
      int pSeqLengthSubGraphEntailmentThreshold, int pSeqLengthSubGraphIncomparabilityThreshold) {
    seqLengthSubGraphEqualityThreshold = pSeqLengthSubGraphEqualityThreshold;
    seqLengthSubGraphEntailmentThreshold = pSeqLengthSubGraphEntailmentThreshold;
    seqLengthSubGraphIncomparabilityThreshold = pSeqLengthSubGraphIncomparabilityThreshold;
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg, SMGState pSMGState,
      Set<SMGAbstractionBlock> pAbstractionLocks) throws SMGInconsistentException {
    SMGJoinDllProgress progress = new SMGJoinDllProgress();

    for (SMGObject object : pSmg.getHeapObjects()) {
      startTraversal(object, pSmg, pSMGState, progress);
    }

    Set<SMGDoublyLinkedListCandidateSequenceBlock> dllBlocks =
        FluentIterable.from(pAbstractionLocks).filter((SMGAbstractionBlock block) -> {
          return block instanceof SMGDoublyLinkedListCandidateSequenceBlock;
        }).transform((SMGAbstractionBlock block) -> {
          return (SMGDoublyLinkedListCandidateSequenceBlock) block;
        }).toSet();

    return progress.getValidCandidates(seqLengthSubGraphEqualityThreshold,
        seqLengthSubGraphEntailmentThreshold, seqLengthSubGraphIncomparabilityThreshold, pSmg, dllBlocks);
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg, SMGState pSmgState)
      throws SMGInconsistentException {
    return traverse(pSmg, pSmgState, ImmutableSet.of());
  }

  private void startTraversal(SMGObject pObject, CLangSMG pSmg, SMGState pSmgState,
      SMGJoinDllProgress pProgress) throws SMGInconsistentException {
    if (pProgress.containsCandidateMap(pObject)) {
      // Processed already in continueTraversal
      return;
    }

    pProgress.putCandidiateMap(pObject);
    createCandidatesOfObject(pObject, pSmg, pSmgState, pProgress);
  }

  private void createCandidatesOfObject(SMGObject pObject, CLangSMG pSmg, SMGState pSMGState,
      SMGJoinDllProgress pProgress) throws SMGInconsistentException {

    if (!pSmg.isObjectValid(pObject)) {
      return;
    }

    if(!(pObject.getKind() == SMGObjectKind.DLL || pObject.getKind() == SMGObjectKind.REG)) {
      return;
    }

    Set<SMGEdgeHasValue> hvesOfObject = pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    if (hvesOfObject.size() < 2) {
      return;
    }

    for (SMGEdgeHasValue hveNext : hvesOfObject) {

      int nfo = hveNext.getOffset();
      CType nfoType = hveNext.getType();
      int nextPointer = hveNext.getValue();

      if (!pSmg.isPointer(nextPointer)) {
        continue;
      }

      SMGEdgePointsTo nextPointerEdge = pSmg.getPointer(nextPointer);
      int hfo = nextPointerEdge.getOffset();
      SMGTargetSpecifier nextPointerTg = nextPointerEdge.getTargetSpecifier();

      if (!(nextPointerTg == SMGTargetSpecifier.REGION
          || nextPointerTg == SMGTargetSpecifier.FIRST)) {
        continue;
      }

      SMGObject nextObject = nextPointerEdge.getObject();

      Set<SMGEdgeHasValue> nextObjectHves = pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject));

      if (nextObjectHves.size() < 2) {
        continue;
      }

      if(pObject == nextObject) {
        continue;
      }

      if (pObject.getSize() != nextObject.getSize()) {
        continue;
      }

      if (!pSmg.isObjectValid(nextObject) || !(nextObject.getLevel() == pObject.getLevel())) {
        continue;
      }

      if(!(nextObject.getKind() == SMGObjectKind.DLL || nextObject.getKind() == SMGObjectKind.REG)) {
        continue;
      }

      for (SMGEdgeHasValue hvePrev : nextObjectHves) {

        int pfo = hvePrev.getOffset();
        CType pfoType = hvePrev.getType();
        int prevPointer = hvePrev.getValue();

        if(!(nfo < pfo)) {
          continue;
        }

        if (!pSmg.isPointer(prevPointer)) {
          continue;
        }

        SMGEdgePointsTo prevPointerEdge = pSmg.getPointer(prevPointer);

        if(prevPointerEdge.getOffset() != hfo) {
          continue;
        }

        SMGTargetSpecifier prevPointerTg = prevPointerEdge.getTargetSpecifier();

        if (!(prevPointerTg == SMGTargetSpecifier.REGION
            || prevPointerTg == SMGTargetSpecifier.LAST)) {
          continue;
        }

        if(pObject != prevPointerEdge.getObject()) {
          continue;
        }

        //TODO At the moment, we still demand that a pointer is found at prev or next.

        Set<SMGEdgeHasValue> prevObjectprevPointer =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject).filterAtOffset(pfo));

        if (prevObjectprevPointer.size() != 1) {
          continue;
        }

        if (!pSmg.isPointer(Iterables.getOnlyElement(prevObjectprevPointer).getValue())) {
          continue;
        }

        SMGDoublyLinkedListCandidate candidate =
            new SMGDoublyLinkedListCandidate(
                pObject, hfo, pfo, nfo, pfoType, nfoType, pSmg.getMachineModel());
        pProgress.initializeCandidiate(candidate);
        continueTraversal(nextPointer, candidate, pSmg, pSMGState, pProgress);
      }
    }
  }

  private void continueTraversal(int pValue, SMGDoublyLinkedListCandidate pPrevCandidate,
      CLangSMG pSmg, SMGState pSmgState, SMGJoinDllProgress pProgress)
      throws SMGInconsistentException {

    // the next object is doubly linked with the prev object, which was checked in start traversal.
    SMGEdgePointsTo pt = pSmg.getPointer(pValue);
    SMGObject nextObject = pt.getObject();
    SMGObject startObject = pPrevCandidate.getObject();

    // First, calculate the longest mergeable sequence of the next object
    if(!pSmg.isHeapObject(nextObject)) {
      return;
    }

    if (!pProgress.containsCandidateMap(nextObject)) {
      startTraversal(nextObject, pSmg, pSmgState, pProgress);
    }

    Integer nfo = pPrevCandidate.getNfo();
    Integer pfo = pPrevCandidate.getPfo();
    Integer hfo = pPrevCandidate.getHfo();

    SMGDoublyLinkedListCandidate candidate;

    if (!pProgress.containsCandidate(nextObject, nfo, pfo)) {
      /* candidate not doubly linked with next object,
       * last object in sequence.
       */

      if (!pSmg.isObjectValid(nextObject) || !(nextObject.getLevel() == startObject.getLevel())) {
        return;
      }

      if(!(nextObject.getKind() == SMGObjectKind.DLL || nextObject.getKind() == SMGObjectKind.REG)) {
        return;
      }

      //TODO At the moment, we still demand that a pointer is found at prev or next.

      Set<SMGEdgeHasValue> nextObjectNextField =
          pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(nfo));

      if(nextObjectNextField.size() != 1) {
        return;
      }

      SMGEdgeHasValue nfoField = Iterables.getOnlyElement(nextObjectNextField);
      SMGEdgeHasValue pfoField = Iterables.getOnlyElement(pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(pfo)));

      if (!pSmg.isPointer(nfoField.getValue())) {
        return;
      }

      candidate = new SMGDoublyLinkedListCandidate(nextObject, hfo, pfo, nfo, pfoField.getType(), nfoField.getType(), pSmg.getMachineModel());
      pProgress.initializeLastInSequenceCandidate(candidate);
    } else {
      candidate = pProgress.getCandidate(nextObject, nfo, pfo);
    }

    if (candidate.getHfo() != pPrevCandidate.getHfo()) {
      return;
    }

    // Second, find out if the subsmgs are mergeable
    SMGJoinSubSMGsForAbstraction join = new SMGJoinSubSMGsForAbstraction(new CLangSMG(pSmg),
        startObject, nextObject, candidate, pSmgState);

    if(!join.isDefined()) {
      return;
    }

    Set<SMGObject> nonSharedObject1 = join.getNonSharedObjectsFromSMG1();
    Set<Integer> nonSharedValues1 = join.getNonSharedValuesFromSMG1();
    Set<SMGObject> nonSharedObject2 = join.getNonSharedObjectsFromSMG2();
    Set<Integer> nonSharedValues2 = join.getNonSharedValuesFromSMG2();

    Set<SMGObject> objectsOfSubSmg1 = new HashSet<>();
    Set<SMGObject> objectsOfSubSmg2 = new HashSet<>();
    Set<Integer> valuesOfSubSmg1 = new HashSet<>();
    Set<Integer> valuesOfSubSmg2 = new HashSet<>();

    getSubSmgOf(startObject, nfo, pfo, pSmg, valuesOfSubSmg1, objectsOfSubSmg1);
    getSubSmgOf(nextObject, nfo, pfo, pSmg, valuesOfSubSmg2, objectsOfSubSmg2);

    objectsOfSubSmg1.remove(startObject);
    objectsOfSubSmg2.remove(nextObject);

    if(nonSharedValues2.contains(pValue)) {
      nonSharedValues2.remove(pValue);
    }

    int prevValue = Iterables.getOnlyElement(pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(pfo))).getValue();

    if(nonSharedValues1.contains(prevValue)) {
      nonSharedValues1.remove(prevValue);
    }

    // Third, calculate if the respective nfo,pfo restricted subsmgs are only reachable from their candidate objects
    if (!isSubSmgSeperate(nonSharedObject1, nonSharedValues1, pSmg, objectsOfSubSmg1,
        valuesOfSubSmg1, startObject)) {
      return;
    }

    if (!isSubSmgSeperate(nonSharedObject2, nonSharedValues2, pSmg, objectsOfSubSmg2,
        valuesOfSubSmg2, nextObject)) {
      return;
    }

    // check if the sequence is uninterrupted
    Set<SMGEdgePointsTo> ptes1 = SMGUtils.getPointerToThisObject(startObject, pSmg);
    Set<SMGEdgePointsTo> ptes2 = SMGUtils.getPointerToThisObject(nextObject, pSmg);


    for (SMGEdgePointsTo pte : ptes1) {
      if (pte.getOffset() != candidate.getHfo()) {
        if (!nonSharedValues1.contains(pte.getValue())) {
          return;
        }
      } else if (startObject.getKind() == SMGObjectKind.DLL
          && pte.getTargetSpecifier() == SMGTargetSpecifier.LAST) {
        Set<SMGEdgeHasValue> prevs = pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        if(prevs.size() != 1) {
          return;
        }
      }
    }

    boolean hasToBeLastInSequence = false;
    SMGJoinStatus joinStatus = join.getStatus();

    for (SMGEdgePointsTo pte : ptes2) {
      if (pte.getOffset() != candidate.getHfo()) {
        if (!nonSharedValues2.contains(pte.getValue())) {
          return;
        }
      } else if (nextObject.getKind() == SMGObjectKind.DLL
          && pte.getTargetSpecifier() == SMGTargetSpecifier.FIRST) {
        Set<SMGEdgeHasValue> prevs =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        if (prevs.size() != 1) {
          return;
        }
      } else if (nextObject.getKind() == SMGObjectKind.REG
          && !hasToBeLastInSequence) {
        Set<SMGEdgeHasValue> hves =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        /* If we want to continue abstracting in this sequence there may be only these two edges, and the edges from the subSmg.*/
        int count = 0;
        for (SMGEdgeHasValue hve : hves) {
          if (!nonSharedObject2.contains(hve.getObject())) {
            count = count + 1;
          }
        }

        if (count != 2) {
          hasToBeLastInSequence = true;
          joinStatus = SMGJoinStatus.RIGHT_ENTAIL;
        }
      }
    }

    pProgress.updateProgress(pPrevCandidate, candidate, joinStatus, hasToBeLastInSequence);
  }

  private boolean isSubSmgSeperate(Set<SMGObject> nonSharedObject, Set<Integer> nonSharedValues,
      CLangSMG smg, Set<SMGObject> reachableObjects, Set<Integer> reachableValues, SMGObject rootOfSubSmg) {

    for (SMGObject obj : nonSharedObject) {

      if(obj.equals(rootOfSubSmg)) {
        continue;
      }

      if (!smg.isHeapObject(obj)) {
        return false;
      }

      Set<SMGEdgePointsTo> pointer = SMGUtils.getPointerToThisObject(obj, smg);

      for (SMGEdgePointsTo pte : pointer) {
        if (!reachableValues.contains(pte.getValue())) {
          return false;
        }
      }
    }

    for (Integer val : nonSharedValues) {
      Set<SMGEdgeHasValue> hves =
          smg.getHVEdges(new SMGEdgeHasValueFilter().filterHavingValue(val));

      /*Abstract simple fields when joining.*/
      if (!smg.isPointer(val)) {
        continue;
      }

      for (SMGEdgeHasValue hve : hves) {
        if (!reachableObjects.contains(hve.getObject()) && hve.getObject() != rootOfSubSmg) {
          return false;
        }
      }
    }

    return true;
  }

  private void getSubSmgOf(SMGObject pObject, int nfo, int pfo, CLangSMG inputSmg,
      Set<Integer> pValues, Set<SMGObject> pObjects) {

    Set<SMGObject> toBeChecked = new HashSet<>();

    pObjects.add(pObject);

    Set<SMGEdgeHasValue> hves = inputSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    for (SMGEdgeHasValue hve : hves) {

      if (hve.getOffset() != pfo && hve.getOffset() != nfo) {

        int subSmgValue = hve.getValue();

        if (!pValues.contains(subSmgValue)) {
          pValues.add(subSmgValue);
        }

        if (inputSmg.isPointer(subSmgValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = inputSmg.getPointer(subSmgValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();

          if (!pObjects.contains(reachedObjectSubSmg)) {
            pObjects.add(reachedObjectSubSmg);
            toBeChecked.add(reachedObjectSubSmg);
          }
        }
      }
    }

    Set<SMGObject> toCheck = new HashSet<>();

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        getSubSmgOf(objToCheck, toBeChecked, inputSmg, pObjects, pValues);
      }
    }
  }

  private void getSubSmgOf(SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked,  CLangSMG pInputSmg, Set<SMGObject> pObjects, Set<Integer> pValues) {

    Set<SMGEdgeHasValue> hves = pInputSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {

      int subDlsValue = hve.getValue();

      if (!pValues.contains(subDlsValue)) {
        pValues.add(subDlsValue);
      }

      if (pInputSmg.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = pInputSmg.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();

        if ((!pObjects.contains(reachedObjectSubSmg))) {
          pObjects.add(reachedObjectSubSmg);
          pToBeChecked.add(reachedObjectSubSmg);
        }
      }
    }
  }

  private static class SMGJoinDllProgress {

    private final Map<SMGObject, Map<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate>> candidates =
        new HashMap<>();
    private final Map<Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus>, Integer> candidateLength =
        new HashMap<>();

    public SMGJoinDllProgress() {

    }

    public void updateProgress(SMGDoublyLinkedListCandidate pPrevCandidate,
        SMGDoublyLinkedListCandidate pCandidate, SMGJoinStatus pStatus,
        boolean pHasToBeLastInSequence) {

      if (pHasToBeLastInSequence) {
        /*Due to the nature of the algorithm, this is always the second segment to be joined from the start*/
        assert candidateLength.get(Pair.of(pPrevCandidate, SMGJoinStatus.EQUAL)) == 1;
        candidateLength.put(Pair.of(pPrevCandidate, pStatus), 2);
        return;
      }

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

    private void updateIncomparableSegment(SMGDoublyLinkedListCandidate pCandidate,
        SMGDoublyLinkedListCandidate pPrevCandidate) {
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> equalLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.EQUAL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> lELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> rELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> incLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE);

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

    private void updateLEntailSegment(SMGDoublyLinkedListCandidate pCandidate,
        SMGDoublyLinkedListCandidate pPrevCandidate) {
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> equalLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.EQUAL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> lELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> rELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> incLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE);

      if (candidateLength.containsKey(equalLengthKey) || candidateLength.containsKey(lELengthKey)) {
        int length = getMaxLength(equalLengthKey, lELengthKey);
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.LEFT_ENTAIL), length + 1);
      }

      if (candidateLength.containsKey(rELengthKey) || candidateLength.containsKey(incLengthKey)) {
        int length = getMaxLength(rELengthKey, incLengthKey);
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE), length + 1);
      }
    }

    private void updateREntailSegment(SMGDoublyLinkedListCandidate pCandidate,
        SMGDoublyLinkedListCandidate pPrevCandidate) {

      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> equalLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.EQUAL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> lELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> rELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
      Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> incLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE);

      if (candidateLength.containsKey(equalLengthKey) || candidateLength.containsKey(rELengthKey)) {
        int length = getMaxLength(equalLengthKey, rELengthKey);
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.RIGHT_ENTAIL), length + 1);
      }

      if (candidateLength.containsKey(lELengthKey) || candidateLength.containsKey(incLengthKey)) {
        int length = getMaxLength(lELengthKey, incLengthKey);
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE), length + 1);
      }
    }

    private int getMaxLength(Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> pKey1,
        Pair<SMGDoublyLinkedListCandidate, SMGJoinStatus> pKey2) {

      int length1 = candidateLength.containsKey(pKey1) ? candidateLength.get(pKey1) : 0;
      int length2 = candidateLength.containsKey(pKey2) ? candidateLength.get(pKey2) : 0;

      return Math.max(length1, length2);
    }

    private void updateEqualSegment(SMGDoublyLinkedListCandidate pCandidate,
        SMGDoublyLinkedListCandidate pPrevCandidate) {

      if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.EQUAL))) {
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.EQUAL),
            candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.EQUAL)) + 1);
      }

      if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL))) {
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.LEFT_ENTAIL),
            candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL)) + 1);
      }

      if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL))) {
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.RIGHT_ENTAIL),
            candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL)) + 1);
      }

      if (candidateLength.containsKey(Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE))) {
        candidateLength.put(Pair.of(pPrevCandidate, SMGJoinStatus.INCOMPARABLE),
            candidateLength.get(Pair.of(pCandidate, SMGJoinStatus.INCOMPARABLE)) + 1);
      }
    }

    public SMGDoublyLinkedListCandidate getCandidate(SMGObject pObject, Integer pNfo,
        Integer pPfo) {
      return candidates.get(pObject).get(Pair.of(pNfo, pPfo));
    }

    public void initializeLastInSequenceCandidate(SMGDoublyLinkedListCandidate pCandidate) {
      candidateLength.put(Pair.of(pCandidate, SMGJoinStatus.EQUAL), 1);
    }

    public boolean containsCandidate(SMGObject pObject, Integer pNfo, Integer pPfo) {

      if (candidates.containsKey(pObject)) {
        return candidates.get(pObject).containsKey(Pair.of(pNfo, pPfo));
      }

      return false;
    }

    public void initializeCandidiate(SMGDoublyLinkedListCandidate pCandidate) {
      candidates.get(pCandidate.getObject()).put(Pair.of(pCandidate.getNfo(), pCandidate.getPfo()), pCandidate);
      candidateLength.put(Pair.of(pCandidate, SMGJoinStatus.EQUAL), 1);
    }

    public void putCandidiateMap(SMGObject pObject) {
      assert !candidates.containsKey(pObject);

      Map<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate> newMap = new HashMap<>();

      candidates.put(pObject, newMap);
    }

    public boolean containsCandidateMap(SMGObject pObject) {
      return candidates.containsKey(pObject);
    }

    public Set<SMGAbstractionCandidate> getValidCandidates(int pSeqLengthSubGraphEqualityThreshold,
        int pSeqLengthSubGraphEntailmentThreshold,
        int pSeqLengthSubGraphIncomparabilityThreshold, CLangSMG pSMG,
        Set<SMGDoublyLinkedListCandidateSequenceBlock> pDllBlocks) throws SMGInconsistentException {

      Set<SMGAbstractionCandidate> resultBeforeBlocks = new HashSet<>();

      for (Map<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate> objCandidates : candidates.values()) {
        for (SMGDoublyLinkedListCandidate candidate : objCandidates.values()) {
          if (candidateLength.containsKey(Pair.of(candidate, SMGJoinStatus.EQUAL))) {
            int length = candidateLength.get(Pair.of(candidate, SMGJoinStatus.EQUAL));
            if (length >= pSeqLengthSubGraphEqualityThreshold) {
              resultBeforeBlocks.add(getCandidat(candidate, length, SMGJoinStatus.EQUAL, pSMG));
            }
          }

          if (candidateLength.containsKey(Pair.of(candidate, SMGJoinStatus.LEFT_ENTAIL))) {
            int length = candidateLength.get(Pair.of(candidate, SMGJoinStatus.LEFT_ENTAIL));
            if (length >= pSeqLengthSubGraphEntailmentThreshold) {
              resultBeforeBlocks.add(getCandidat(candidate, length, SMGJoinStatus.LEFT_ENTAIL, pSMG));
            }
          }

          if (candidateLength.containsKey(Pair.of(candidate, SMGJoinStatus.RIGHT_ENTAIL))) {
            int length = candidateLength.get(Pair.of(candidate, SMGJoinStatus.RIGHT_ENTAIL));
            if (length >= pSeqLengthSubGraphEntailmentThreshold) {
              resultBeforeBlocks.add(getCandidat(candidate, length, SMGJoinStatus.RIGHT_ENTAIL, pSMG));
            }
          }

          if (candidateLength.containsKey(Pair.of(candidate, SMGJoinStatus.INCOMPARABLE))) {
            int length = candidateLength.get(Pair.of(candidate, SMGJoinStatus.INCOMPARABLE));
            if (length >= pSeqLengthSubGraphIncomparabilityThreshold) {
              resultBeforeBlocks.add(getCandidat(candidate, length, SMGJoinStatus.INCOMPARABLE, pSMG));
            }
          }
        }
      }

      if (pDllBlocks.isEmpty()) {
        return resultBeforeBlocks;
      }

      Set<SMGAbstractionCandidate> result = new HashSet<>(resultBeforeBlocks.size());

      for (SMGAbstractionCandidate candidate : resultBeforeBlocks) {
        boolean blocked = false;
        for (SMGDoublyLinkedListCandidateSequenceBlock block : pDllBlocks) {
          if (block.isBlocked(candidate, pSMG)) {
            blocked = true;
            break;
          }
        }

        if (!blocked) {
          result.add(candidate);
        }
      }

      return result;
    }

    private SMGDoublyLinkedListCandidateSequence getCandidat(
        SMGDoublyLinkedListCandidate pCandidate, int length, SMGJoinStatus status, CLangSMG pSMG) {
      return new SMGDoublyLinkedListCandidateSequence(pCandidate, length, status, isDllPartOfSequence(pCandidate, length, pSMG));
    }

    private boolean isDllPartOfSequence(SMGDoublyLinkedListCandidate pCandidate, int pLength,
        CLangSMG pSMG) {

      SMGObject nextObject = pCandidate.getObject();

      if (nextObject.getKind() == SMGObjectKind.DLL) {
        return true;
      }

      for (int i = 1; i < pLength; i++) {


        SMGEdgeHasValue nextHveEdge = Iterables.getOnlyElement(pSMG.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(pCandidate.getNfo())));
        nextObject = pSMG.getPointer(nextHveEdge.getValue()).getObject();

        if (nextObject.getKind() == SMGObjectKind.DLL) {
          return true;
        }
      }

      return false;
    }

    @Override
    public String toString() {
      return "SMGJoinDllProgress [candidates=" + candidates + ", candidateLength=" + candidateLength
          + "]";
    }
  }
}