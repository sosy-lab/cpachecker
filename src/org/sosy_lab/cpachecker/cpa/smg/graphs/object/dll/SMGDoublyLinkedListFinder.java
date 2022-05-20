// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionFinder;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinSubSMGsForAbstraction;
import org.sosy_lab.cpachecker.util.Pair;

public class SMGDoublyLinkedListFinder extends SMGAbstractionFinder {

  @VisibleForTesting
  public SMGDoublyLinkedListFinder() {}

  public SMGDoublyLinkedListFinder(
      int pSeqLengthEqualityThreshold,
      int pSeqLengthEntailmentThreshold,
      int pSeqLengthIncomparableThreshold) {
    super(
        pSeqLengthEqualityThreshold,
        pSeqLengthEntailmentThreshold,
        pSeqLengthIncomparableThreshold);
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(
      UnmodifiableCLangSMG pSmg,
      UnmodifiableSMGState pSMGState,
      Set<SMGAbstractionBlock> pAbstractionBlocks)
      throws SMGInconsistentException {
    SMGJoinDllProgress progress = new SMGJoinDllProgress();

    for (SMGObject object : pSmg.getHeapObjects()) {
      startTraversal(object, pSmg, pSMGState, progress);
    }

    Set<SMGDoublyLinkedListCandidateSequenceBlock> dllBlocks =
        FluentIterable.from(pAbstractionBlocks)
            .filter(SMGDoublyLinkedListCandidateSequenceBlock.class)
            .toSet();

    return progress.getValidCandidates(
        seqLengthEqualityThreshold,
        seqLengthEntailmentThreshold,
        seqLengthIncomparableThreshold,
        pSmg,
        dllBlocks);
  }

  private void startTraversal(
      SMGObject pObject,
      UnmodifiableCLangSMG pSmg,
      UnmodifiableSMGState pSmgState,
      SMGJoinDllProgress pProgress)
      throws SMGInconsistentException {
    if (pProgress.containsCandidateMap(pObject)) {
      // Processed already in continueTraversal
      return;
    }

    createCandidatesOfObject(pObject, pSmg, pSmgState, pProgress);
  }

  private void createCandidatesOfObject(
      SMGObject pObject,
      UnmodifiableCLangSMG pSmg,
      UnmodifiableSMGState pSMGState,
      SMGJoinDllProgress pProgress)
      throws SMGInconsistentException {

    if (!pSmg.isObjectValid(pObject)) {
      return;
    }

    if (!(pObject.getKind() == SMGObjectKind.DLL || pObject.getKind() == SMGObjectKind.REG)) {
      return;
    }

    SMGHasValueEdges hvesOfObject = pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    if (hvesOfObject.size() < 2) {
      return;
    }

    for (final SMGEdgeHasValue hveNext : hvesOfObject) {

      SMGValue nextPointer = hveNext.getValue();
      if (!pSmg.isPointer(nextPointer)) {
        continue;
      }

      SMGEdgePointsTo nextPointerEdge = pSmg.getPointer(nextPointer);
      long hfo = nextPointerEdge.getOffset();
      SMGTargetSpecifier nextPointerTg = nextPointerEdge.getTargetSpecifier();
      if (!(nextPointerTg == SMGTargetSpecifier.REGION
          || nextPointerTg == SMGTargetSpecifier.FIRST)) {
        continue;
      }

      SMGObject nextObject = nextPointerEdge.getObject();

      SMGHasValueEdges nextObjectHves =
          pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject));

      if (nextObjectHves.size() < 2) {
        continue;
      }

      if (pObject == nextObject) {
        continue;
      }

      if (pObject.getSize() != nextObject.getSize()) {
        continue;
      }

      if (!pSmg.isObjectValid(nextObject) || !(nextObject.getLevel() == pObject.getLevel())) {
        continue;
      }

      if (!(nextObject.getKind() == SMGObjectKind.DLL
          || nextObject.getKind() == SMGObjectKind.REG)) {
        continue;
      }

      long nfo = hveNext.getOffset();
      for (SMGEdgeHasValue hvePrev : nextObjectHves) {

        long pfo = hvePrev.getOffset();

        if (!(nfo < pfo)) {
          continue;
        }

        SMGValue prevPointer = hvePrev.getValue();
        if (!pSmg.isPointer(prevPointer)) {
          continue;
        }

        SMGEdgePointsTo prevPointerEdge = pSmg.getPointer(prevPointer);

        if (prevPointerEdge.getOffset() != hfo) {
          continue;
        }

        SMGTargetSpecifier prevPointerTg = prevPointerEdge.getTargetSpecifier();

        if (!(prevPointerTg == SMGTargetSpecifier.REGION
            || prevPointerTg == SMGTargetSpecifier.LAST)) {
          continue;
        }

        if (pObject != prevPointerEdge.getObject()) {
          continue;
        }

        // TODO At the moment, we still demand that a pointer is found at prev or next.

        Iterable<SMGEdgeHasValue> prevObjectprevPointer =
            pSmg.getHVEdges(
                SMGEdgeHasValueFilter.objectFilter(pObject)
                    .filterAtOffset(pfo)
                    .filterBySize(pSmg.getSizeofPtrInBits()));

        if (Iterables.size(prevObjectprevPointer) != 1) {
          continue;
        }

        if (!pSmg.isPointer(Iterables.getOnlyElement(prevObjectprevPointer).getValue())) {
          continue;
        }

        SMGDoublyLinkedListCandidate candidate =
            new SMGDoublyLinkedListCandidate(
                pObject,
                pObject,
                hfo,
                pfo,
                nfo,
                hvePrev.getSizeInBits(),
                hveNext.getSizeInBits(),
                pSmg.getMachineModel());
        pProgress.initializeCandidiate(candidate);
        continueTraversal(nextPointer, candidate, pSmg, pSMGState, pProgress);
      }
    }
  }

  private void continueTraversal(
      SMGValue pValue,
      SMGDoublyLinkedListCandidate pPrevCandidate,
      UnmodifiableCLangSMG pSmg,
      UnmodifiableSMGState pSmgState,
      SMGJoinDllProgress pProgress)
      throws SMGInconsistentException {

    // the next object is doubly linked with the prev object, which was checked in start traversal.
    SMGEdgePointsTo pt = pSmg.getPointer(pValue);
    SMGObject nextObject = pt.getObject();
    SMGObject startObject = pPrevCandidate.getStartObject();

    // First, calculate the longest mergeable sequence of the next object
    if (!pSmg.isHeapObject(nextObject)) {
      return;
    }

    if (!pProgress.containsCandidateMap(nextObject)) {
      startTraversal(nextObject, pSmg, pSmgState, pProgress);
    }

    Long nfo = pPrevCandidate.getShape().getNfo();
    Long pfo = pPrevCandidate.getShape().getPfo();
    Long hfo = pPrevCandidate.getShape().getHfo();

    SMGDoublyLinkedListCandidate candidate;

    if (!pProgress.containsCandidate(nextObject, Pair.of(nfo, pfo))) {
      /* candidate not doubly linked with next object,
       * last object in sequence.
       */

      if (!pSmg.isObjectValid(nextObject) || !(nextObject.getLevel() == startObject.getLevel())) {
        return;
      }

      if (!(nextObject.getKind() == SMGObjectKind.DLL
          || nextObject.getKind() == SMGObjectKind.REG)) {
        return;
      }

      // TODO At the moment, we still demand that a pointer is found at prev or next.

      Iterable<SMGEdgeHasValue> nextObjectNextField =
          pSmg.getHVEdges(
              SMGEdgeHasValueFilter.objectFilter(nextObject)
                  .filterAtOffset(nfo)
                  .filterBySize(pSmg.getSizeofPtrInBits()));

      if (Iterables.size(nextObjectNextField) != 1) {
        return;
      }

      SMGEdgeHasValue nfoField = Iterables.getOnlyElement(nextObjectNextField);
      SMGEdgeHasValue pfoField =
          Iterables.getOnlyElement(
              pSmg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(nextObject)
                      .filterAtOffset(pfo)
                      .filterBySize(pSmg.getSizeofPtrInBits())));

      if (!pSmg.isPointer(nfoField.getValue())) {
        return;
      }

      candidate =
          new SMGDoublyLinkedListCandidate(
              nextObject,
              nextObject,
              hfo,
              pfo,
              nfo,
              pfoField.getSizeInBits(),
              nfoField.getSizeInBits(),
              pSmg.getMachineModel());
      pProgress.initializeLastInSequenceCandidate(candidate);
    } else {
      candidate = pProgress.getCandidate(nextObject, Pair.of(nfo, pfo));
    }

    if (candidate.getLastObject().equals(startObject)) {
      return;
    }

    if (candidate.getShape().getHfo() != pPrevCandidate.getShape().getHfo()) {
      return;
    }

    // Second, find out if the subsmgs are mergeable
    SMGJoinSubSMGsForAbstraction join =
        new SMGJoinSubSMGsForAbstraction(
            pSmg.copyOf(), startObject, nextObject, candidate, pSmgState);

    if (!join.isDefined()) {
      return;
    }

    Set<SMGObject> nonSharedObject1 = join.getNonSharedObjectsFromSMG1();
    Set<SMGValue> nonSharedValues1 = join.getNonSharedValuesFromSMG1();
    Set<SMGObject> nonSharedObject2 = join.getNonSharedObjectsFromSMG2();
    Set<SMGValue> nonSharedValues2 = join.getNonSharedValuesFromSMG2();

    Set<SMGObject> objectsOfSubSmg1 = new HashSet<>();
    Set<SMGObject> objectsOfSubSmg2 = new HashSet<>();
    Set<SMGValue> valuesOfSubSmg1 = new HashSet<>();
    Set<SMGValue> valuesOfSubSmg2 = new HashSet<>();

    Predicate<SMGEdgeHasValue> check = hve -> hve.getOffset() != pfo && hve.getOffset() != nfo;
    getSubSmgOf(startObject, check, pSmg, valuesOfSubSmg1, objectsOfSubSmg1);
    getSubSmgOf(nextObject, check, pSmg, valuesOfSubSmg2, objectsOfSubSmg2);

    objectsOfSubSmg1.remove(startObject);
    objectsOfSubSmg2.remove(nextObject);

    nonSharedValues2.remove(pValue);

    SMGValue prevValue =
        Iterables.getOnlyElement(
                pSmg.getHVEdges(
                    SMGEdgeHasValueFilter.objectFilter(nextObject)
                        .filterAtOffset(pfo)
                        .filterWithoutSize()))
            .getValue();

    nonSharedValues1.remove(prevValue);

    // Third, calculate if the respective nfo,pfo restricted subsmgs are only reachable from their
    // candidate objects
    if (!isSubSmgSeperate(
        nonSharedObject1, nonSharedValues1, pSmg, objectsOfSubSmg1, valuesOfSubSmg1, startObject)) {
      return;
    }

    if (!isSubSmgSeperate(
        nonSharedObject2, nonSharedValues2, pSmg, objectsOfSubSmg2, valuesOfSubSmg2, nextObject)) {
      return;
    }

    // check if the sequence is uninterrupted
    Set<SMGEdgePointsTo> ptes1 = SMGUtils.getPointerToThisObject(startObject, pSmg);
    Set<SMGEdgePointsTo> ptes2 = SMGUtils.getPointerToThisObject(nextObject, pSmg);

    for (SMGEdgePointsTo pte : ptes1) {
      if (pte.getOffset() != candidate.getShape().getHfo()) {
        if (!nonSharedValues1.contains(pte.getValue())) {
          return;
        }
      } else if (startObject.getKind() == SMGObjectKind.DLL
          && pte.getTargetSpecifier() == SMGTargetSpecifier.LAST) {
        Iterable<SMGEdgeHasValue> prevs =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        if (Iterables.size(prevs) != 1) {
          return;
        }
      }
    }

    boolean hasToBeLastInSequence = false;
    SMGJoinStatus joinStatus = join.getStatus();

    for (SMGEdgePointsTo pte : ptes2) {
      if (pte.getOffset() != candidate.getShape().getHfo()) {
        if (!nonSharedValues2.contains(pte.getValue())) {
          return;
        }
      } else if (nextObject.getKind() == SMGObjectKind.DLL
          && pte.getTargetSpecifier() == SMGTargetSpecifier.FIRST) {
        Iterable<SMGEdgeHasValue> prevs =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pte.getValue()));

        if (Iterables.size(prevs) != 1) {
          return;
        }
      } else if (nextObject.getKind() == SMGObjectKind.REG && !hasToBeLastInSequence) {
        Iterable<SMGEdgeHasValue> hves =
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
}
