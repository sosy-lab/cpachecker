// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import com.google.common.collect.Iterables;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractListCandidateSequence;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinSubSMGsForAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPathCollector;

public class SMGSingleLinkedListCandidateSequence
    extends SMGAbstractListCandidateSequence<SMGSingleLinkedListCandidate> {

  public SMGSingleLinkedListCandidateSequence(
      SMGSingleLinkedListCandidate pCandidate,
      int pLength,
      SMGJoinStatus pSmgJoinStatus,
      boolean pIncludesSll) {
    super(pCandidate, pLength, pSmgJoinStatus, pIncludesSll);
  }

  @Override
  public CLangSMG execute(CLangSMG pSMG, SMGState pSmgState) throws SMGInconsistentException {
    SMGObject prevObject = candidate.getStartObject();
    long nfo = candidate.getShape().getNfo();

    pSmgState.pruneUnreachable();

    // Abstraction not reachable
    if (!pSMG.getHeapObjects().contains(prevObject)) {
      return pSMG;
    }

    for (int i = 1; i < length; i++) {

      SMGEdgeHasValue nextEdge =
          Iterables.getOnlyElement(
              pSMG.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(prevObject)
                      .filterAtOffset(nfo)
                      .filterBySize(pSMG.getSizeofPtrInBits())));
      SMGObject nextObject = pSMG.getPointer(nextEdge.getValue()).getObject();

      if (nextObject == prevObject) {
        throw new AssertionError("Invalid candidate sequence: Attempt to merge object with itself");
      }

      if (length > 1) {
        SMGJoinSubSMGsForAbstraction jointest =
            new SMGJoinSubSMGsForAbstraction(
                pSMG.copyOf(), prevObject, nextObject, candidate, pSmgState);

        if (!jointest.isDefined()) {
          return pSMG;
        }
      }

      SMGJoinSubSMGsForAbstraction join =
          new SMGJoinSubSMGsForAbstraction(pSMG, prevObject, nextObject, candidate, pSmgState);

      if (!join.isDefined()) {
        throw new AssertionError(
            "Unexpected join failure while abstracting longest mergeable sequence");
      }

      SMGObject newAbsObj = join.getNewAbstractObject();

      for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(nextObject, pSMG)) {
        pSMG.removePointsToEdge(pte.getValue());

        if (pte.getTargetSpecifier() == SMGTargetSpecifier.ALL) {
          SMGEdgePointsTo newPte =
              new SMGEdgePointsTo(
                  pte.getValue(), newAbsObj, pte.getOffset(), SMGTargetSpecifier.ALL);
          pSMG.addPointsToEdge(newPte);
        }
      }

      addPointsToEdges(pSMG, prevObject, newAbsObj, SMGTargetSpecifier.FIRST);

      SMGEdgeHasValue nextObj2hve =
          Iterables.getOnlyElement(
              pSMG.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(nextObject)
                      .filterAtOffset(nfo)
                      .filterBySize(pSMG.getSizeofPtrInBits())));

      for (SMGObject obj : join.getNonSharedObjectsFromSMG1()) {
        pSMG.markHeapObjectDeletedAndRemoveEdges(obj);
      }

      for (SMGObject obj : join.getNonSharedObjectsFromSMG2()) {
        pSMG.markHeapObjectDeletedAndRemoveEdges(obj);
      }

      pSMG.markHeapObjectDeletedAndRemoveEdges(nextObject);
      pSMG.markHeapObjectDeletedAndRemoveEdges(prevObject);
      prevObject = newAbsObj;

      SMGEdgeHasValue nfoHve =
          new SMGEdgeHasValue(
              nextObj2hve.getSizeInBits(),
              nextObj2hve.getOffset(),
              newAbsObj,
              nextObj2hve.getValue());
      pSMG.addHasValueEdge(nfoHve);
      pSmgState.pruneUnreachable();

      replaceSourceValues(pSMG, newAbsObj);
    }

    return pSMG;
  }

  @Override
  public String toString() {
    return "SMGSingleLinkedListCandidateSequence [candidate="
        + candidate
        + ", length="
        + length
        + "]";
  }

  @Override
  public SMGAbstractionBlock createAbstractionBlock(UnmodifiableSMGState pSmgState) {
    Map<SMGObject, SMGMemoryPath> map =
        new SMGMemoryPathCollector(pSmgState.getHeap()).getHeapObjectMemoryPaths();
    SMGMemoryPath pPointerToStartObject = map.get(candidate.getStartObject());
    return new SMGSingleLinkedListCandidateSequenceBlock(
        candidate.getShape(), length, pPointerToStartObject);
  }
}
