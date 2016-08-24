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
import org.sosy_lab.cpachecker.cpa.smg.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGLevelMapping.SMGJoinLevel;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedListCandidate;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;


final public class SMGJoinSubSMGsForAbstraction {

  private final SMGJoinStatus status;
  private final CLangSMG resultSMG;
  private final SMGObject newAbstractObject;
  private final Set<Integer> nonSharedValuesFromSMG1;
  private final Set<Integer> nonSharedValuesFromSMG2;
  private final Set<SMGObject> nonSharedObjectsFromSMG1;
  private final Set<SMGObject> nonSharedObjectsFromSMG2;
  private final boolean defined;

  public SMGJoinSubSMGsForAbstraction(CLangSMG pInputSMG, SMGObject obj1, SMGObject obj2, SMGListCandidate pListCandidate, SMGState pStateOfSmg) throws SMGInconsistentException {

    CLangSMG smg = pInputSMG;
    Set<SMGObject> origObjects = ImmutableSet.copyOf(smg.getObjects());
    Set<Integer> origValues = ImmutableSet.copyOf(smg.getValues());

    int nfo;
    int pfo;
    int hfo;

    SMGEdgeHasValue prevObj1hve = null;
    SMGEdgeHasValue nextObj1hve = null;
    SMGEdgeHasValue prevObj2hve = null;
    SMGEdgeHasValue nextObj2hve = null;

    if (pListCandidate instanceof SMGDoublyLinkedListCandidate) {
      SMGDoublyLinkedListCandidate dllc = (SMGDoublyLinkedListCandidate) pListCandidate;
      nfo = dllc.getNfo();
      pfo = dllc.getPfo();
      hfo = dllc.getHfo();

      int lengthObj1 = getMinLength(obj1);
      int lengthObj2 = getMinLength(obj2);

      int length = lengthObj1 + lengthObj2;
      SMGObject dll = new SMGDoublyLinkedList(obj1.getSize(), hfo, nfo, pfo, length, obj1.getLevel());
      smg.addHeapObject(dll);
      newAbstractObject = dll;

      prevObj1hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(pfo)));
      nextObj1hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(nfo)));
      prevObj2hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(pfo)));
      nextObj2hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(nfo)));

      smg.removeHasValueEdge(prevObj1hve);
      smg.removeHasValueEdge(nextObj1hve);
      smg.removeHasValueEdge(prevObj2hve);
      smg.removeHasValueEdge(nextObj2hve);

    } else {
      SMGSingleLinkedListCandidate sllc = (SMGSingleLinkedListCandidate) pListCandidate;
      hfo = sllc.getHfo();
      nfo = sllc.getNfo();

      int lengthObj1 = getMinLength(obj1);
      int lengthObj2 = getMinLength(obj2);

      int length = lengthObj1 + lengthObj2;
      SMGObject sll = new SMGSingleLinkedList(obj1.getSize(), hfo, nfo, length, obj1.getLevel());
      smg.addHeapObject(sll);
      newAbstractObject = sll;

      nextObj1hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(nfo)));
      nextObj2hve = Iterables.getOnlyElement(smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(nfo)));

      smg.removeHasValueEdge(nextObj1hve);
      smg.removeHasValueEdge(nextObj2hve);
    }

    int lDiff = 0;

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    mapping1.map(obj1, newAbstractObject);
    mapping2.map(obj2, newAbstractObject);

    int destLevel = obj1.getLevel();

    if (shouldAbstractionIncreaseLevel(obj1, obj2)) {
      destLevel = destLevel + 1;
    }

    CLangSMG inputSMG = new CLangSMG(smg);

    /*Every value thats identical will be skipped, the join only iterates over non shared values, thats why we can introduce a
     * level map only for non shared objects*/
    SMGLevelMapping levelMap = new SMGLevelMapping();
    levelMap.put(SMGJoinLevel.valueOf(obj1.getLevel(), obj2.getLevel()), destLevel);

    SMGJoinSubSMGs jss = new SMGJoinSubSMGs(SMGJoinStatus.EQUAL, inputSMG, inputSMG, smg, mapping1, mapping2, levelMap, obj1, obj2, newAbstractObject, lDiff, true, pStateOfSmg, pStateOfSmg);

    if(!jss.isDefined()) {
      status = SMGJoinStatus.INCOMPLETE;
      defined = false;
      resultSMG = null;
      nonSharedObjectsFromSMG1 = null;
      nonSharedObjectsFromSMG2 = null;
      nonSharedValuesFromSMG1 = null;
      nonSharedValuesFromSMG2 = null;
      return;
    }

    SMGJoinStatus s = jss.getStatus();
    mapping1 = jss.getMapping1();
    mapping2 = jss.getMapping2();

    //TODO Contains abstract 0Cycle?

    boolean bothObjectsAreRegOrOpt = shouldAbstractionIncreaseLevel(obj1, obj2);

    if (bothObjectsAreRegOrOpt) {
      for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(newAbstractObject, smg)) {
        smg.removePointsToEdge(pte.getValue());
        smg.addPointsToEdge(new SMGEdgePointsTo(pte.getValue(), pte.getObject(), pte.getOffset(), SMGTargetSpecifier.ALL));
      }
    }

    if (newAbstractObject.getKind() == SMGObjectKind.DLL) {
      smg.addHasValueEdge(prevObj1hve);
      smg.addHasValueEdge(prevObj2hve);
    }

    smg.addHasValueEdge(nextObj1hve);
    smg.addHasValueEdge(nextObj2hve);

    defined = true;
    status = s;
    resultSMG = smg;

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

        /*Beware identical values, they are shared.*/
        if (nonSharedValuesFromSMG1.contains(entry.getKey())) {
          nonSharedValuesFromSMG1.remove(entry.getKey());
        } else {
          nonSharedValuesFromSMG2.add(entry.getKey());
        }
      }
    }

    // Zero is not a non shared value
    nonSharedValuesFromSMG1.remove(0);
    nonSharedValuesFromSMG2.remove(0);
  }

  private boolean shouldAbstractionIncreaseLevel(SMGObject pObj1, SMGObject pObj2) {

    switch (pObj1.getKind()) {
      case REG:
      case OPTIONAL:
        switch (pObj2.getKind()) {
          case REG:
          case OPTIONAL:
            return true;
          default:
            return false;
        }

      default:
        return false;
    }
  }

  private int getMinLength(SMGObject pObj) {

    switch (pObj.getKind()) {
      case REG:
        return 1;
      case DLL:
        return ((SMGDoublyLinkedList) pObj).getMinimumLength();
      case SLL:
        return ((SMGSingleLinkedList) pObj).getMinimumLength();
      case OPTIONAL:
        return 0;
      default:
        throw new AssertionError();
    }
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