// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.annotations.VisibleForTesting;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public abstract class SMGAbstractionFinder {

  protected final int seqLengthEqualityThreshold;
  protected final int seqLengthEntailmentThreshold;
  protected final int seqLengthIncomparableThreshold;

  @VisibleForTesting // some default values for testing, constructor never used otherwise
  protected SMGAbstractionFinder() {
    seqLengthEqualityThreshold = 2;
    seqLengthEntailmentThreshold = 2;
    seqLengthIncomparableThreshold = 3;
  }

  protected SMGAbstractionFinder(
      int pSeqLengthEqualityThreshold,
      int pSeqLengthEntailmentThreshold,
      int pSeqLengthIncomparableThreshold) {
    seqLengthEqualityThreshold = pSeqLengthEqualityThreshold;
    seqLengthEntailmentThreshold = pSeqLengthEntailmentThreshold;
    seqLengthIncomparableThreshold = pSeqLengthIncomparableThreshold;
  }

  public abstract Set<SMGAbstractionCandidate> traverse(
      UnmodifiableCLangSMG pSmg,
      UnmodifiableSMGState pSMGState,
      Set<SMGAbstractionBlock> abstractionBlocks)
      throws SMGInconsistentException;

  protected boolean isSubSmgSeperate(
      Set<SMGObject> nonSharedObject,
      Set<SMGValue> nonSharedValues,
      UnmodifiableCLangSMG smg,
      Set<SMGObject> reachableObjects,
      Set<SMGValue> reachableValues,
      SMGObject rootOfSubSmg) {

    for (SMGObject obj : nonSharedObject) {

      if (obj.equals(rootOfSubSmg)) {
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

    for (SMGValue val : nonSharedValues) {

      /*Abstract simple fields when joining.*/
      if (!smg.isPointer(val)) {
        continue;
      }

      for (SMGEdgeHasValue hve : smg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(val))) {
        if (!reachableObjects.contains(hve.getObject()) && hve.getObject() != rootOfSubSmg) {
          return false;
        }
      }
    }

    return true;
  }

  protected final void getSubSmgOf(
      SMGObject pObject,
      Predicate<SMGEdgeHasValue> check,
      UnmodifiableCLangSMG inputSmg,
      Set<SMGValue> pValues,
      Set<SMGObject> pObjects) {

    Set<SMGObject> toBeChecked = new LinkedHashSet<>();

    pObjects.add(pObject);

    for (SMGEdgeHasValue hve : inputSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject))) {
      if (check.test(hve)) {

        SMGValue subSmgValue = hve.getValue();
        pValues.add(subSmgValue);

        if (inputSmg.isPointer(subSmgValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = inputSmg.getPointer(subSmgValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();

          if (pObjects.add(reachedObjectSubSmg)) {
            toBeChecked.add(reachedObjectSubSmg);
          }
        }
      }
    }

    Set<SMGObject> toCheck = new LinkedHashSet<>();

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        getSubSmgOf(objToCheck, toBeChecked, inputSmg, pObjects, pValues);
      }
    }
  }

  private void getSubSmgOf(
      SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked,
      UnmodifiableCLangSMG pInputSmg,
      Set<SMGObject> pObjects,
      Set<SMGValue> pValues) {

    for (SMGEdgeHasValue hve :
        pInputSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck))) {
      SMGValue subDlsValue = hve.getValue();
      pValues.add(subDlsValue);

      if (pInputSmg.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = pInputSmg.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();

        if (pObjects.add(reachedObjectSubSmg)) {
          pToBeChecked.add(reachedObjectSubSmg);
        }
      }
    }
  }
}
