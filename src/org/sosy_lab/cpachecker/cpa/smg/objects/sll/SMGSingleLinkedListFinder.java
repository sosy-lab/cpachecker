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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionFinder;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import com.google.common.collect.Iterables;

public class SMGSingleLinkedListFinder implements SMGAbstractionFinder {
  private CLangSMG smg;
  private Map<SMGObject, Map<Integer, SMGSingleLinkedListCandidate>> candidates = new HashMap<>();
  private Map<Integer, Integer> inboundPointers = new HashMap<>();

  final private int seqLengthThreshold;

  public SMGSingleLinkedListFinder() {
    seqLengthThreshold = 10;
  }

  public SMGSingleLinkedListFinder(int pSeqLengthThreshold) {
    seqLengthThreshold = pSeqLengthThreshold;
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg) {
    smg = pSmg;

    buildInboundPointers();

    for (SMGObject object : smg.getHeapObjects()) {
      startTraversal(object);
    }

    Set<SMGAbstractionCandidate> returnSet = new HashSet<>();
    for (Map<Integer, SMGSingleLinkedListCandidate> objCandidates : candidates.values()) {
      for (SMGSingleLinkedListCandidate candidate : objCandidates.values()) {
        if (candidate.getLength() > seqLengthThreshold) {
          returnSet.add(candidate);
        }
      }
    }
    return Collections.unmodifiableSet(returnSet);
  }

  private void buildInboundPointers() {
    for (Integer pointer : smg.getPTEdges().keySet()) {
      inboundPointers.put(pointer, Iterables.size(smg.getHVEdges(new SMGEdgeHasValueFilter().filterHavingValue(pointer))));
    }
  }

  private void startTraversal(SMGObject pObject) {
    if (candidates.containsKey(pObject)) {
      // Processed already in continueTraversal
      return;
    }
    candidates.put(pObject, new HashMap<Integer, SMGSingleLinkedListCandidate>());
    for (SMGEdgeHasValue hv : smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject))) {
      if (smg.isPointer(hv.getValue())) {
        SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(pObject, hv.getOffset(), 1);
        candidates.get(pObject).put(hv.getOffset(), candidate);
        continueTraversal(hv.getValue(), candidate);
      }
    }
  }

  private void continueTraversal(int pValue, SMGSingleLinkedListCandidate pCandidate) {
    SMGEdgePointsTo pt = smg.getPointer(pValue);
    SMGObject object = pt.getObject();
    if (! candidates.containsKey(object)) {
      startTraversal(object);
    }

    if (inboundPointers.get(pValue) > 1) {
      return;
    }

    Map<Integer, SMGSingleLinkedListCandidate> objectCandidates = candidates.get(object);
    Integer offset = pCandidate.getOffset();

    if (! objectCandidates.containsKey(offset)) {
      //try to infer a pointer presence: either NULL, or uninitialized
      if (smg.isCoveredByNullifiedBlocks(object, offset, AnonymousTypes.dummyPointer)) {
        objectCandidates.put(offset, new SMGSingleLinkedListCandidate(object, offset, 1));
      }
    }

    if (objectCandidates.containsKey(offset)) {
      SMGSingleLinkedListCandidate myCandidate = objectCandidates.get(offset);
      if(pCandidate.isCompatibleWith(myCandidate)) {
        objectCandidates.remove(offset);
        pCandidate.addLength(myCandidate.getLength());
      }
    }
  }
}