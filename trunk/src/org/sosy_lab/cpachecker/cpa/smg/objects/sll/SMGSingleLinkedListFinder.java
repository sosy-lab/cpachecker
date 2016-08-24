/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

public class SMGSingleLinkedListFinder implements SMGAbstractionFinder {

  private final int seqLengthEqualityThreshold;
  private final int seqLengthEntailmentThreshold;
  private final int seqLengthIncomparableThreshold;

  public SMGSingleLinkedListFinder() {
    seqLengthEqualityThreshold = 2;
    seqLengthEntailmentThreshold = 2;
    seqLengthIncomparableThreshold = 3;
  }

  public SMGSingleLinkedListFinder(int pSeqLengthEqualityThreshold,
      int pSeqLengthEntailmentThreshold, int pSeqLengthIncomparableThreshold) {
    seqLengthEqualityThreshold = pSeqLengthEqualityThreshold;
    seqLengthEntailmentThreshold = pSeqLengthEntailmentThreshold;
    seqLengthIncomparableThreshold = pSeqLengthIncomparableThreshold;
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg, SMGState pSMGState,
      Set<SMGAbstractionBlock> pAbstractionLocks) throws SMGInconsistentException {
    SMGJoinSllProgress pProgress = new SMGJoinSllProgress();

    for (SMGObject object : pSmg.getHeapObjects()) {
      startTraversal(object, pSmg, pSMGState, pProgress);
    }

    Set<SMGSingleLinkedListCandidateSequenceBlock> sllBlocks =
        FluentIterable.from(pAbstractionLocks).filter((SMGAbstractionBlock block) -> {
          return block instanceof SMGSingleLinkedListCandidateSequenceBlock;
        }).transform((SMGAbstractionBlock block) -> {
          return (SMGSingleLinkedListCandidateSequenceBlock) block;
        }).toSet();

    return pProgress.getValidCandidates(seqLengthEqualityThreshold, seqLengthEntailmentThreshold, seqLengthIncomparableThreshold, pSmg, sllBlocks);
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg, SMGState pSMGState) throws SMGInconsistentException {
    return traverse(pSmg, pSMGState, ImmutableSet.of());
  }

  private void startTraversal(SMGObject pObject, CLangSMG pSmg, SMGState pSmgState,
      SMGJoinSllProgress pProgress) throws SMGInconsistentException {
    if (pProgress.containsCandidateMap(pObject)) {
      // Processed already in continueTraversal
      return;
    }

    pProgress.putCandidiateMap(pObject);
    createCandidatesOfObject(pObject, pSmg, pSmgState, pProgress);
  }

  private void createCandidatesOfObject(SMGObject pObject, CLangSMG pSmg, SMGState pSMGState, SMGJoinSllProgress pProgress) throws SMGInconsistentException {

    if (!pSmg.isObjectValid(pObject)) {
      return;
    }

    if(!(pObject.getKind() == SMGObjectKind.SLL || pObject.getKind() == SMGObjectKind.REG || pObject.getKind() == SMGObjectKind.OPTIONAL)) {
      return;
    }

    Set<SMGEdgeHasValue> hvesOfObject = pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    for (SMGEdgeHasValue hveNext : hvesOfObject) {

      int nfo = hveNext.getOffset();
      int nextPointer = hveNext.getValue();

      if (!pSmg.isPointer(nextPointer)) {
        continue;
      }

      SMGEdgePointsTo nextPointerEdge = pSmg.getPointer(nextPointer);
      int hfo = nextPointerEdge.getOffset();
      SMGTargetSpecifier nextPointerTg = nextPointerEdge.getTargetSpecifier();

      if (!(nextPointerTg == SMGTargetSpecifier.REGION
          || nextPointerTg == SMGTargetSpecifier.FIRST
          || nextPointerTg == SMGTargetSpecifier.OPT)) {
        continue;
      }

      SMGObject nextObject = nextPointerEdge.getObject();

      if(pObject == nextObject) {
        continue;
      }

      if (pObject.getSize() != nextObject.getSize()) {
        continue;
      }

      if(!(nextObject.getKind() == SMGObjectKind.SLL || nextObject.getKind() == SMGObjectKind.REG || nextObject.getKind() == SMGObjectKind.OPTIONAL)) {
        continue;
      }

      if (!pSmg.isObjectValid(nextObject) || !(nextObject.getLevel() == pObject.getLevel())) {
        continue;
      }

      Set<SMGEdgePointsTo> pointsToThis = SMGUtils.getPointerToThisObject(pObject, pSmg);

      Set<CType> typesOfThisObject = new HashSet<>();

      for (SMGEdgePointsTo edge : pointsToThis) {
        Set<SMGEdgeHasValue> hves =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(edge.getValue()));
        for (SMGEdgeHasValue hve : hves) {
          typesOfThisObject.add(hve.getType());
        }
      }

      CType nextType = hveNext.getType();

      if (!typesOfThisObject.contains(nextType)) {
        continue;
      }

      SMGSingleLinkedListCandidate candidate =
          new SMGSingleLinkedListCandidate(pObject, nfo, hfo, nextType,
              pSmg.getMachineModel());
      pProgress.initializeCandidiate(candidate);
      continueTraversal(nextPointer, candidate, pSmg, pSMGState, pProgress);
    }
  }

  private void continueTraversal(int pValue, SMGSingleLinkedListCandidate pPrevCandidate,
      CLangSMG pSmg, SMGState pSmgState, SMGJoinSllProgress pProgress) throws SMGInconsistentException {

    SMGEdgePointsTo pt = pSmg.getPointer(pValue);
    SMGObject nextObject = pt.getObject();
    SMGObject startObject = pPrevCandidate.getStartObject();

    // First, calculate the longest mergeable sequence of the next object
    if(!pSmg.isHeapObject(nextObject)) {
      return;
    }

    if (!pProgress.containsCandidateMap(nextObject)) {
      startTraversal(nextObject, pSmg, pSmgState, pProgress);
    }

    Integer nfo = pPrevCandidate.getNfo();
    Integer hfo = pPrevCandidate.getHfo();

    SMGSingleLinkedListCandidate candidate;

    if (!pProgress.containsCandidate(nextObject, nfo)) {
      /* candidate not doubly linked with next object,
       * last object in sequence.
       */

      if (!pSmg.isObjectValid(nextObject) || !(nextObject.getLevel() == startObject.getLevel())) {
        return;
      }

      if(!(nextObject.getKind() == SMGObjectKind.SLL || nextObject.getKind() == SMGObjectKind.REG || nextObject.getKind() == SMGObjectKind.OPTIONAL)) {
        return;
      }

      //TODO At the moment, we still demand that a value is found at prev or next.

      Set<SMGEdgeHasValue> nextObjectNextPointer =
          pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(nfo));

      if (nextObjectNextPointer.size() != 1) {
        return;
      }

      SMGEdgeHasValue nextEdge = Iterables.getOnlyElement(nextObjectNextPointer);

      if (!pSmg.isPointer(nextEdge.getValue())) {
        return;
      }

      candidate = new SMGSingleLinkedListCandidate(nextObject, nfo, hfo, nextEdge.getType(),
          pSmg.getMachineModel());
      pProgress.initializeLastInSequenceCandidate(candidate);

    } else {
      candidate = pProgress.getCandidate(nextObject, nfo);
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

    getSubSmgOf(startObject, nfo, pSmg, valuesOfSubSmg1, objectsOfSubSmg1);
    getSubSmgOf(nextObject, nfo, pSmg, valuesOfSubSmg2, objectsOfSubSmg2);

    objectsOfSubSmg1.remove(startObject);
    objectsOfSubSmg2.remove(nextObject);

    // TODO Investigate, is this okay?
    if(nonSharedValues2.contains(pValue)) {
      nonSharedValues2.remove(pValue);
    }

    // Third, calculate if the respective nfo restricted subsmgs are only reachable from their candidate objects
    if (!isSubSmgSeperate(nonSharedObject1, nonSharedValues1, pSmg, objectsOfSubSmg1,
        valuesOfSubSmg1, startObject)) {
      isSubSmgSeperate(nonSharedObject1, nonSharedValues1, pSmg, objectsOfSubSmg1,
          valuesOfSubSmg1, startObject);
      return;
    }

    if (!isSubSmgSeperate(nonSharedObject2, nonSharedValues2, pSmg, objectsOfSubSmg2,
        valuesOfSubSmg2, nextObject)) {
      isSubSmgSeperate(nonSharedObject2, nonSharedValues2, pSmg, objectsOfSubSmg2,
          valuesOfSubSmg2, nextObject);
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
      }
    }

    for (SMGEdgePointsTo pte : ptes2) {
      if (pte.getOffset() != candidate.getHfo()) {
        if (!nonSharedValues2.contains(pte.getValue())) {
          return;
        }
      } else {

        /* Nothing besides the one link from the prev object pointer may
         * point to the next object in a sll
         */
        Set<SMGEdgeHasValue> prevs =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        if (prevs.size() != 1) {
          return;
        }
      }
    }

    pProgress.updateProgress(pPrevCandidate, candidate, join.getStatus());
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

      /*Abstract simple fields when joining.*/
      if (!smg.isPointer(val)) {
        continue;
      }

      Set<SMGEdgeHasValue> hves =
          smg.getHVEdges(new SMGEdgeHasValueFilter().filterHavingValue(val));

      for (SMGEdgeHasValue hve : hves) {
        if (!reachableObjects.contains(hve.getObject()) && hve.getObject() != rootOfSubSmg) {
          return false;
        }
      }
    }

    return true;
  }

  private void getSubSmgOf(SMGObject pObject, int nfo, CLangSMG inputSmg,
      Set<Integer> pValues, Set<SMGObject> pObjects) {

    Set<SMGObject> toBeChecked = new HashSet<>();

    pObjects.add(pObject);

    Set<SMGEdgeHasValue> hves = inputSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    for (SMGEdgeHasValue hve : hves) {

      if (hve.getOffset() != nfo) {

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

  private static class SMGJoinSllProgress {

    private final Map<SMGObject, Map<Integer, SMGSingleLinkedListCandidate>> candidates =
        new HashMap<>();
    private final Map<Pair<SMGSingleLinkedListCandidate, SMGJoinStatus>, Integer> candidateLength =
        new HashMap<>();

    public SMGJoinSllProgress() {

    }

    public void updateProgress(SMGSingleLinkedListCandidate pPrevCandidate,
        SMGSingleLinkedListCandidate pCandidate, SMGJoinStatus pStatus) {

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

    private void updateIncomparableSegment(SMGSingleLinkedListCandidate pCandidate,
        SMGSingleLinkedListCandidate pPrevCandidate) {
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> equalLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.EQUAL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> lELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> rELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> incLengthKey =
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

    private void updateLEntailSegment(SMGSingleLinkedListCandidate pCandidate,
        SMGSingleLinkedListCandidate pPrevCandidate) {
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> equalLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.EQUAL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> lELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> rELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> incLengthKey =
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

    private void updateREntailSegment(SMGSingleLinkedListCandidate pCandidate,
        SMGSingleLinkedListCandidate pPrevCandidate) {

      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> equalLengthKey =
          Pair.of(pCandidate, SMGJoinStatus.EQUAL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> lELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.LEFT_ENTAIL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> rELengthKey =
          Pair.of(pCandidate, SMGJoinStatus.RIGHT_ENTAIL);
      Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> incLengthKey =
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

    private int getMaxLength(Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> pKey1,
        Pair<SMGSingleLinkedListCandidate, SMGJoinStatus> pKey2) {

      int length1 = candidateLength.containsKey(pKey1) ? candidateLength.get(pKey1) : 0;
      int length2 = candidateLength.containsKey(pKey2) ? candidateLength.get(pKey2) : 0;

      return Math.max(length1, length2);
    }

    private void updateEqualSegment(SMGSingleLinkedListCandidate pCandidate,
        SMGSingleLinkedListCandidate pPrevCandidate) {

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

    public SMGSingleLinkedListCandidate getCandidate(SMGObject pObject, Integer pNfo) {
      return candidates.get(pObject).get(pNfo);
    }

    public void initializeLastInSequenceCandidate(SMGSingleLinkedListCandidate pCandidate) {
      candidateLength.put(Pair.of(pCandidate, SMGJoinStatus.EQUAL), 1);
    }

    public boolean containsCandidate(SMGObject pObject, Integer pNfo) {

      if (candidates.containsKey(pObject)) {
        return candidates.get(pObject).containsKey(pNfo);
      }

      return false;
    }

    public void initializeCandidiate(SMGSingleLinkedListCandidate pCandidate) {
      candidates.get(pCandidate.getStartObject()).put(pCandidate.getNfo(), pCandidate);
      candidateLength.put(Pair.of(pCandidate, SMGJoinStatus.EQUAL), 1);
    }

    public void putCandidiateMap(SMGObject pObject) {
      assert !candidates.containsKey(pObject);

      Map<Integer, SMGSingleLinkedListCandidate> newMap = new HashMap<>();

      candidates.put(pObject, newMap);
    }

    public boolean containsCandidateMap(SMGObject pObject) {
      return candidates.containsKey(pObject);
    }

    public Set<SMGAbstractionCandidate> getValidCandidates(int pSeqLengthSubGraphEqualityThreshold,
        int pSeqLengthSubGraphEntailmentThreshold,
        int pSeqLengthSubGraphIncomparabilityThreshold, CLangSMG pSMG,
        Set<SMGSingleLinkedListCandidateSequenceBlock> pSllBlocks) throws SMGInconsistentException {

      Set<SMGAbstractionCandidate> resultBeforeBlocks = new HashSet<>();

      for (Map<Integer, SMGSingleLinkedListCandidate> objCandidates : candidates.values()) {
        for (SMGSingleLinkedListCandidate candidate : objCandidates.values()) {
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

      if (pSllBlocks.isEmpty()) {
        return resultBeforeBlocks;
      }

      Set<SMGAbstractionCandidate> result = new HashSet<>(resultBeforeBlocks.size());

      for (SMGAbstractionCandidate candidate : resultBeforeBlocks) {
        boolean blocked = false;
        for (SMGSingleLinkedListCandidateSequenceBlock block : pSllBlocks) {
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

    private SMGSingleLinkedListCandidateSequence getCandidat(
        SMGSingleLinkedListCandidate pCandidate, int length, SMGJoinStatus status, CLangSMG pSMG) {
      return new SMGSingleLinkedListCandidateSequence(pCandidate, length, status, isPartOfSllSequence(pCandidate, length, pSMG));
    }

    private boolean isPartOfSllSequence(SMGSingleLinkedListCandidate pCandidate, int pLength,
        CLangSMG pSmg) {

      SMGObject nextObject = pCandidate.getStartObject();

      if (nextObject.getKind() == SMGObjectKind.SLL) {
        return true;
      }

      for (int i = 1; i < pLength; i++) {
        SMGEdgeHasValue nextHveEdge = Iterables.getOnlyElement(pSmg.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(pCandidate.getNfo())));
        nextObject = pSmg.getPointer(nextHveEdge.getValue()).getObject();

        if (nextObject.getKind() == SMGObjectKind.SLL) {
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