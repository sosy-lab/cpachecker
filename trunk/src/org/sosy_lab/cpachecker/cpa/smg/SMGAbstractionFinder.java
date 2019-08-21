/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public abstract class SMGAbstractionFinder {

  protected final int seqLengthEqualityThreshold;
  protected final int seqLengthEntailmentThreshold;
  protected final int seqLengthIncomparableThreshold;

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

  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg, UnmodifiableSMGState pSMGState)
      throws SMGInconsistentException {
    return traverse(pSmg, pSMGState, ImmutableSet.of());
  }

  public abstract Set<SMGAbstractionCandidate> traverse(
      CLangSMG pSmg, UnmodifiableSMGState pSMGState, Set<SMGAbstractionBlock> abstractionBlocks)
      throws SMGInconsistentException;

  protected boolean isSubSmgSeperate(
      Set<SMGObject> nonSharedObject,
      Set<SMGValue> nonSharedValues,
      CLangSMG smg,
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
      CLangSMG inputSmg,
      Set<SMGValue> pValues,
      Set<SMGObject> pObjects) {

    Set<SMGObject> toBeChecked = new HashSet<>();

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

  private void getSubSmgOf(
      SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked,
      CLangSMG pInputSmg,
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