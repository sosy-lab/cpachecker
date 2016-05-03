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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SMGSingleLinkedListFinder implements SMGAbstractionFinder {

  private CLangSMG smg;
  private  Map<SMGObject, Map<Integer, SMGSingleLinkedListCandidate>> candidates = new HashMap<>();
  private  Map<SMGSingleLinkedListCandidate, Integer> candidateLength = new HashMap<>();
  private  Map<SMGSingleLinkedListCandidate, SMGJoinStatus> candidateSeqJoinStatus = new HashMap<>();

  private final int seqLengthThreshold;

  public SMGSingleLinkedListFinder() {
    seqLengthThreshold = 2;
  }

  public SMGSingleLinkedListFinder(int pSeqLengthThreshold) {
    seqLengthThreshold = pSeqLengthThreshold;
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg, SMGState pSMGState) throws SMGInconsistentException {
    smg = pSmg;

    candidateLength.clear();
    candidates.clear();
    candidateSeqJoinStatus.clear();

    for (SMGObject object : smg.getHeapObjects()) {
      startTraversal(object, pSMGState);
    }

    Set<SMGAbstractionCandidate> returnSet = new HashSet<>();
    for (Map<Integer, SMGSingleLinkedListCandidate> objCandidates : candidates.values()) {
      for (SMGSingleLinkedListCandidate candidate : objCandidates.values()) {
        if (candidateLength.get(candidate) >= seqLengthThreshold
            || (candidateSeqJoinStatus.get(candidate) != SMGJoinStatus.INCOMPARABLE
                && candidateLength.get(candidate) > 0)) {
          returnSet.add(new SMGSingleLinkedListCandidateSequence(candidate, candidateLength.get(candidate), candidateSeqJoinStatus.get(candidate)));
        }
      }
    }

    return Collections.unmodifiableSet(returnSet);
  }

  private void startTraversal(SMGObject pObject, SMGState pSmgState) throws SMGInconsistentException {
    if (candidates.containsKey(pObject)) {
      // Processed already in continueTraversal
      return;
    }

    candidates.put(pObject, new HashMap<Integer, SMGSingleLinkedListCandidate>());
    createCandidatesOfObject(pObject, pSmgState);
  }

  private void createCandidatesOfObject(SMGObject pObject, SMGState pSMGState) throws SMGInconsistentException {

    if (!smg.isObjectValid(pObject)) {
      return;
    }

    if(!(pObject.getKind() == SMGObjectKind.SLL || pObject.getKind() == SMGObjectKind.REG || pObject.getKind() == SMGObjectKind.OPTIONAL)) {
      return;
    }

    Set<SMGEdgeHasValue> hvesOfObject = smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    for (SMGEdgeHasValue hveNext : hvesOfObject) {

      int nfo = hveNext.getOffset();
      int nextPointer = hveNext.getValue();

      if (!smg.isPointer(nextPointer)) {
        continue;
      }

      SMGEdgePointsTo nextPointerEdge = smg.getPointer(nextPointer);
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

      if (!smg.isObjectValid(nextObject) || !(nextObject.getLevel() == pObject.getLevel())) {
        continue;
      }

      SMGSingleLinkedListCandidate candidate =
          new SMGSingleLinkedListCandidate(pObject, nfo, hfo);
      candidates.get(pObject).put(nfo, candidate);
      candidateLength.put(candidate, 0);
      candidateSeqJoinStatus.put(candidate, SMGJoinStatus.EQUAL);
      continueTraversal(nextPointer, candidate, pSMGState);
    }
  }

  private void continueTraversal(int pValue, SMGSingleLinkedListCandidate pPrevCandidate,
      SMGState pSmgState) throws SMGInconsistentException {

    SMGEdgePointsTo pt = smg.getPointer(pValue);
    SMGObject nextObject = pt.getObject();
    SMGObject startObject = pPrevCandidate.getStartObject();

    // First, calculate the longest mergeable sequence of the next object
    if(!smg.isHeapObject(nextObject)) {
      return;
    }

    if (! candidates.containsKey(nextObject)) {
      startTraversal(nextObject, pSmgState);
    }

    Map<Integer, SMGSingleLinkedListCandidate> objectCandidates = candidates.get(nextObject);
    Integer nfo = pPrevCandidate.getNfo();
    Integer hfo = pPrevCandidate.getHfo();

    SMGSingleLinkedListCandidate candidate;

    if (!objectCandidates.containsKey(nfo)) {
      /* candidate not doubly linked with next object,
       * last object in sequence.
       */
      candidate = new SMGSingleLinkedListCandidate(nextObject, nfo, hfo);
      candidateLength.put(candidate, 0);
      candidateSeqJoinStatus.put(candidate, SMGJoinStatus.EQUAL);

      if (!smg.isObjectValid(nextObject) || !(nextObject.getLevel() == startObject.getLevel())) {
        return;
      }

      if(!(nextObject.getKind() == SMGObjectKind.SLL || nextObject.getKind() == SMGObjectKind.REG || nextObject.getKind() == SMGObjectKind.OPTIONAL)) {
        return;
      }

      //TODO At the moment, we still demand that a value is found at prev or next.
      if (smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(nfo))
          .isEmpty()) {
        return;
      }
    } else {
      candidate = objectCandidates.get(nfo);
    }

    if (candidate.getHfo() != pPrevCandidate.getHfo()) {
      return;
    }

    // Second, find out if the subsmgs are mergeable
    SMGJoinSubSMGsForAbstraction join = new SMGJoinSubSMGsForAbstraction(new CLangSMG(smg), startObject, nextObject, candidate, pSmgState);

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

    getSubSmgOf(startObject, nfo, smg, valuesOfSubSmg1, objectsOfSubSmg1);
    getSubSmgOf(nextObject, nfo, smg, valuesOfSubSmg2, objectsOfSubSmg2);

    objectsOfSubSmg1.remove(startObject);
    objectsOfSubSmg2.remove(nextObject);

    // TODO Investigate, is this okay?
    if(nonSharedValues2.contains(pValue)) {
      nonSharedValues2.remove(pValue);
    }

    // Third, calculate if the respective nfo restricted subsmgs are only reachable from their candidate objects
    if (!isSubSmgSeperate(nonSharedObject1, nonSharedValues1, smg, objectsOfSubSmg1,
        valuesOfSubSmg1, startObject)) {
      isSubSmgSeperate(nonSharedObject1, nonSharedValues1, smg, objectsOfSubSmg1,
          valuesOfSubSmg1, startObject);
      return;
    }

    if (!isSubSmgSeperate(nonSharedObject2, nonSharedValues2, smg, objectsOfSubSmg2,
        valuesOfSubSmg2, nextObject)) {
      isSubSmgSeperate(nonSharedObject2, nonSharedValues2, smg, objectsOfSubSmg2,
          valuesOfSubSmg2, nextObject);
      return;
    }

    // check if the sequence is uninterrupted
    Set<SMGEdgePointsTo> ptes1 = SMGUtils.getPointerToThisObject(startObject, smg);
    Set<SMGEdgePointsTo> ptes2 = SMGUtils.getPointerToThisObject(nextObject, smg);


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
            smg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        if (prevs.size() != 1) {
          return;
        }
      }
    }

    candidateLength.put(pPrevCandidate, candidateLength.get(candidate) + 1);
    SMGJoinStatus newSequenceJoinStatus =
        SMGJoinStatus.updateStatus(candidateSeqJoinStatus.get(candidate), join.getStatus());
    candidateSeqJoinStatus.put(pPrevCandidate, newSequenceJoinStatus);
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
}