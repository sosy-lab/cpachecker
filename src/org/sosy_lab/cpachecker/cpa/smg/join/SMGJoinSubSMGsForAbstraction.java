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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedListCandidate;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;


final public class SMGJoinSubSMGsForAbstraction {

  private SMGJoinStatus status = null;
  private CLangSMG resultSMG = null;
  private SMGObject newAbstractObject = null;
  private Set<Integer> nonSharedValuesFromSMG1 = null;
  private Set<Integer> nonSharedValuesFromSMG2 = null;
  private Set<SMGObject> nonSharedObjectsFromSMG1 = null;
  private Set<SMGObject> nonSharedObjectsFromSMG2 = null;
  private boolean defined = false;

  public SMGJoinSubSMGsForAbstraction(CLangSMG pInputSMG, SMGObject obj1, SMGObject obj2, SMGDoublyLinkedListCandidate dlsc, SMGState pStateOfSmg) throws SMGInconsistentException {

    CLangSMG smg = pInputSMG;
    Set<SMGObject> origObjects = ImmutableSet.copyOf(smg.getObjects());
    Set<Integer> origValues = ImmutableSet.copyOf(smg.getValues());

    SMGEdgeHasValue prevObj1hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(dlsc.getPfo())));
    SMGEdgeHasValue nextObj1hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(dlsc.getNfo())));
    SMGEdgeHasValue prevObj2hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(dlsc.getPfo())));
    SMGEdgeHasValue nextObj2hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(dlsc.getNfo())));

    smg.removeHasValueEdge(prevObj1hve);
    smg.removeHasValueEdge(nextObj1hve);
    smg.removeHasValueEdge(prevObj2hve);
    smg.removeHasValueEdge(nextObj2hve);

    int lengthObj1 = obj1 instanceof SMGDoublyLinkedList ? ((SMGDoublyLinkedList)obj1).getMinimumLength() : 1;
    int lengthObj2 = obj2 instanceof SMGDoublyLinkedList ? ((SMGDoublyLinkedList)obj2).getMinimumLength() : 1;

    int length = lengthObj1 + lengthObj2;
    SMGDoublyLinkedList dls = new SMGDoublyLinkedList(obj1.getSize(), dlsc.getHfo(), dlsc.getNfo(), dlsc.getPfo(), length, obj1.getLevel());
    smg.addHeapObject(dls);

    int lDiff = 0;

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    mapping1.map(obj1, dls);
    mapping2.map(obj2, dls);

    boolean increaseLevel =
        obj1 instanceof SMGRegion && obj2 instanceof SMGRegion;

    CLangSMG inputSMG = new CLangSMG(smg);

    SMGJoinSubSMGs jss = new SMGJoinSubSMGs(SMGJoinStatus.EQUAL, inputSMG, inputSMG, smg, mapping1, mapping2, obj1, obj2, dls, lDiff, increaseLevel, true, pStateOfSmg, pStateOfSmg);

    if(!jss.isDefined()) {
      return;
    }

    SMGJoinStatus s = jss.getStatus();
    mapping1 = jss.getMapping1();
    mapping2 = jss.getMapping2();

    //TODO Contains dls0Cycle?

    /* increase level hold already calculated value for
     * obj1 instanceof SMGRegion && obj2 instanceof SMGRegion
     */
    if (increaseLevel) {
      for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(dls, smg)) {
        smg.removePointsToEdge(pte.getValue());
        smg.addPointsToEdge(new SMGEdgePointsTo(pte.getValue(), pte.getObject(), pte.getOffset(), SMGTargetSpecifier.ALL));
      }
    }

    smg.addHasValueEdge(prevObj1hve);
    smg.addHasValueEdge(nextObj1hve);
    smg.addHasValueEdge(prevObj2hve);
    smg.addHasValueEdge(nextObj2hve);

    defined = true;
    status = s;
    resultSMG = smg;
    newAbstractObject = dls;

    nonSharedObjectsFromSMG1 = new HashSet<>();
    nonSharedObjectsFromSMG2 = new HashSet<>();

    nonSharedValuesFromSMG1 = new HashSet<>();
    nonSharedValuesFromSMG2 = new HashSet<>();

    for (Entry<SMGObject, SMGObject> entry : mapping1.getObject_mapEntrySet()) {
      if(origObjects.contains(entry.getKey())) {
        nonSharedObjectsFromSMG1.add(entry.getKey());
      }
    }

    for (Entry<SMGObject, SMGObject> entry : mapping2.getObject_mapEntrySet()) {
      if(origObjects.contains(entry.getKey())) {
        nonSharedObjectsFromSMG2.add(entry.getKey());
      }
    }

    for (Entry<Integer, Integer> entry : mapping1.getValue_mapEntrySet()) {
      if (origValues.contains(entry.getKey())) {
        nonSharedValuesFromSMG1.add(entry.getKey());
      }
    }

    for (Entry<Integer, Integer> entry : mapping2.getValue_mapEntrySet()) {
      if (origValues.contains(entry.getKey())) {
        nonSharedValuesFromSMG2.add(entry.getKey());
      }
    }

    // Zero is not a non shared value
    nonSharedValuesFromSMG1.remove(0);
    nonSharedValuesFromSMG2.remove(0);
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public CLangSMG getResultSMG() {
    return resultSMG;
  }

  public SMGObject getNewAbstractObject() {
    return newAbstractObject;
  }

  public Set<SMGObject> getNonSharedObjectsFromSMG2() {
    return nonSharedObjectsFromSMG2;
  }

  public Set<SMGObject> getNonSharedObjectsFromSMG1() {
    return nonSharedObjectsFromSMG1;
  }

  public Set<Integer> getNonSharedValuesFromSMG1() {
    return nonSharedValuesFromSMG1;
  }

  public Set<Integer> getNonSharedValuesFromSMG2() {
    return nonSharedValuesFromSMG2;
  }
}