// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public final class SMGJoinSubSMGsForAbstraction {

  private final SMGJoinStatus status;
  private final UnmodifiableCLangSMG resultSMG;
  private final SMGObject newAbstractObject;
  private final Set<SMGValue> nonSharedValuesFromSMG1;
  private final Set<SMGValue> nonSharedValuesFromSMG2;
  private final Set<SMGObject> nonSharedObjectsFromSMG1;
  private final Set<SMGObject> nonSharedObjectsFromSMG2;
  private final boolean defined;

  public SMGJoinSubSMGsForAbstraction(
      CLangSMG smg,
      SMGObject obj1,
      SMGObject obj2,
      SMGListCandidate<?> pListCandidate,
      UnmodifiableSMGState pStateOfSmg)
      throws SMGInconsistentException {

    PersistentSet<SMGObject> origObjects = smg.getObjects();
    PersistentSet<SMGValue> origValues = smg.getValues();

    long nfo;
    long pfo;
    long hfo;

    SMGEdgeHasValue prevObj1hve = null;
    SMGEdgeHasValue nextObj1hve = null;
    SMGEdgeHasValue prevObj2hve = null;
    SMGEdgeHasValue nextObj2hve = null;

    if (pListCandidate instanceof SMGDoublyLinkedListCandidate) {
      SMGDoublyLinkedListCandidate dllc = (SMGDoublyLinkedListCandidate) pListCandidate;
      nfo = dllc.getShape().getNfo();
      pfo = dllc.getShape().getPfo();
      hfo = dllc.getShape().getHfo();

      int lengthObj1 = getMinLength(obj1);
      int lengthObj2 = getMinLength(obj2);

      int length = lengthObj1 + lengthObj2;
      SMGObject dll =
          new SMGDoublyLinkedList(obj1.getSize(), hfo, nfo, pfo, length, obj1.getLevel());
      smg.addHeapObject(dll);
      newAbstractObject = dll;

      prevObj1hve =
          Iterables.getOnlyElement(
              smg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(obj1)
                      .filterAtOffset(pfo)
                      .filterBySize(smg.getSizeofPtrInBits())));
      nextObj1hve =
          Iterables.getOnlyElement(
              smg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(obj1)
                      .filterAtOffset(nfo)
                      .filterBySize(smg.getSizeofPtrInBits())));
      prevObj2hve =
          Iterables.getOnlyElement(
              smg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(obj2)
                      .filterAtOffset(pfo)
                      .filterBySize(smg.getSizeofPtrInBits())));
      nextObj2hve =
          Iterables.getOnlyElement(
              smg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(obj2)
                      .filterAtOffset(nfo)
                      .filterBySize(smg.getSizeofPtrInBits())));

      // FIXME: remove only one pointer size
      smg.removeHasValueEdge(prevObj1hve);
      smg.removeHasValueEdge(nextObj1hve);
      smg.removeHasValueEdge(prevObj2hve);
      smg.removeHasValueEdge(nextObj2hve);

    } else {
      SMGSingleLinkedListCandidate sllc = (SMGSingleLinkedListCandidate) pListCandidate;
      hfo = sllc.getShape().getHfo();
      nfo = sllc.getShape().getNfo();

      int lengthObj1 = getMinLength(obj1);
      int lengthObj2 = getMinLength(obj2);

      int length = lengthObj1 + lengthObj2;
      SMGObject sll = new SMGSingleLinkedList(obj1.getSize(), hfo, nfo, length, obj1.getLevel());
      smg.addHeapObject(sll);
      newAbstractObject = sll;

      nextObj1hve =
          Iterables.getOnlyElement(
              smg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(obj1)
                      .filterAtOffset(nfo)
                      .filterBySize(smg.getSizeofPtrInBits())));
      nextObj2hve =
          Iterables.getOnlyElement(
              smg.getHVEdges(
                  SMGEdgeHasValueFilter.objectFilter(obj2)
                      .filterAtOffset(nfo)
                      .filterBySize(smg.getSizeofPtrInBits())));

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

    CLangSMG inputSMG = smg.copyOf();

    /*Every value thats identical will be skipped, the join only iterates over non shared values, thats why we can introduce a
     * level map only for non shared objects*/
    SMGLevelMapping levelMap = new SMGLevelMapping();
    levelMap.put(SMGJoinLevel.valueOf(obj1.getLevel(), obj2.getLevel()), destLevel);

    SMGJoinSubSMGs jss =
        new SMGJoinSubSMGs(
            SMGJoinStatus.EQUAL,
            inputSMG,
            inputSMG,
            smg,
            mapping1,
            mapping2,
            levelMap,
            obj1,
            obj2,
            newAbstractObject,
            lDiff,
            true,
            pStateOfSmg,
            pStateOfSmg);

    if (!jss.isDefined()) {
      status = SMGJoinStatus.INCOMPARABLE;
      defined = false;
      resultSMG = null;
      nonSharedObjectsFromSMG1 = null;
      nonSharedObjectsFromSMG2 = null;
      nonSharedValuesFromSMG1 = null;
      nonSharedValuesFromSMG2 = null;
      return;
    }

    SMGJoinStatus s = jss.getStatus();

    // TODO Contains abstract 0Cycle?

    boolean bothObjectsAreRegOrOpt = shouldAbstractionIncreaseLevel(obj1, obj2);

    if (bothObjectsAreRegOrOpt) {
      for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(newAbstractObject, smg)) {
        smg.removePointsToEdge(pte.getValue());
        smg.addPointsToEdge(
            new SMGEdgePointsTo(
                pte.getValue(), pte.getObject(), pte.getOffset(), SMGTargetSpecifier.ALL));
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
      if (origObjects.contains(entry.getKey())) {
        nonSharedObjectsFromSMG1.add(entry.getKey());
      }
    }

    for (Entry<SMGObject, SMGObject> entry : mapping2.getObject_mapEntrySet()) {
      if (origObjects.contains(entry.getKey())) {
        nonSharedObjectsFromSMG2.add(entry.getKey());
      }
    }

    for (Entry<SMGValue, SMGValue> entry : mapping1.getValue_mapEntrySet()) {
      if (origValues.contains(entry.getKey())) {
        nonSharedValuesFromSMG1.add(entry.getKey());
      }
    }

    for (Entry<SMGValue, SMGValue> entry : mapping2.getValue_mapEntrySet()) {
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
    nonSharedValuesFromSMG1.remove(SMGZeroValue.INSTANCE);
    nonSharedValuesFromSMG2.remove(SMGZeroValue.INSTANCE);
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

  public UnmodifiableCLangSMG getResultSMG() {
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

  public Set<SMGValue> getNonSharedValuesFromSMG1() {
    return nonSharedValuesFromSMG1;
  }

  public Set<SMGValue> getNonSharedValuesFromSMG2() {
    return nonSharedValuesFromSMG2;
  }
}
