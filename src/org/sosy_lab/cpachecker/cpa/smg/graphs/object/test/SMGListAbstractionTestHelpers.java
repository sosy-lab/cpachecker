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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import com.google.common.truth.Truth;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGRuntimeCheck;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

public final class SMGListAbstractionTestHelpers {

  private static SMGValue newVal() {
    return SMGKnownSymValue.of();
  }

  private static SMGValue[] generateNewAddresses(int pCount) {
    if (pCount < 1) {
      throw new IllegalArgumentException("Count must be at least 1.");
    }
    SMGValue[] addresses = new SMGValue[pCount];
    for (int i = 0; i < pCount; i++) {
      addresses[i] = newVal();
    }
    return addresses;
  }

  private static SMGObject[] createRegionsOnHeap(CLangSMG pSmg, int pCount, int pSize) {
    if (pCount < 1) {
      throw new IllegalArgumentException("Count must be at least 1.");
    }
    SMGObject[] regions = new SMGObject[pCount];
    for (int i = 0; i < pCount; i++) {
      SMGObject region = new SMGRegion(pSize, "list_node" + i);
      pSmg.addHeapObject(region);
      regions[i] = region;
    }
    return regions;
  }

  private static SMGObject[] createListsOnHeap(
      CLangSMG pSmg,
      int pCount,
      int pSize,
      int pHfo,
      int pNfo,
      int pPfo,
      Integer[] pMinLengths,
      int pLevel,
      SMGListLinkage pLinkage) {
    if (pCount < 1) {
      throw new IllegalArgumentException("Count must be at least 1.");
    }
    SMGObject[] lists = new SMGObject[pCount];
    for (int i = 0; i < pCount; i++) {
      SMGObject list = null;
      switch (pLinkage) {
        case SINGLE_LINKED:
          list = new SMGSingleLinkedList(pSize, pHfo, pNfo, pMinLengths[i], pLevel);
          break;
        case DOUBLY_LINKED:
          list = new SMGDoublyLinkedList(pSize, pHfo, pNfo, pPfo, pMinLengths[i], pLevel);
          break;
        default:
          throw new IllegalArgumentException("Unsupported linkage type: " + pLinkage);
      }
      pSmg.addHeapObject(list);
      lists[i] = list;
    }
    return lists;
  }

  private static SMGEdgeHasValue[] addFieldsToObjectsOnHeap(
      CLangSMG pSmg, SMGObject[] pObjects, SMGValue[] pValues, int pFieldSize, int pFieldOffset) {
    if (pObjects == null || pValues == null || pObjects.length == 0) {
      throw new IllegalArgumentException("The provided arrays must not be null or empty.");
    }
    if (pObjects.length != pValues.length) {
      throw new IllegalArgumentException(
          "The number of objects must be equal to the number of values.");
    }
    SMGEdgeHasValue[] hvEdges = new SMGEdgeHasValue[pObjects.length];
    for (int i = 0; i < pObjects.length; i++) {
      pSmg.addValue(pValues[i]);
      SMGEdgeHasValue hv = new SMGEdgeHasValue(pFieldSize, pFieldOffset, pObjects[i], pValues[i]);
      pSmg.addHasValueEdge(hv);
      hvEdges[i] = hv;
    }
    return hvEdges;
  }

  private static SMGEdgePointsTo[] addPointersToRegionsOnHeap(
      CLangSMG pSmg, SMGObject[] pRegions, SMGValue[] pAddresses, int pOffset) {
    if (pRegions == null || pAddresses == null || pRegions.length == 0) {
      throw new IllegalArgumentException("The provided arrays must not be null or empty.");
    }
    if (pRegions.length != pAddresses.length) {
      throw new IllegalArgumentException(
          "The number of regions must be equal to the number of addresses.");
    }
    SMGEdgePointsTo[] ptEdges = new SMGEdgePointsTo[pRegions.length];
    for (int i = 0; i < pRegions.length; i++) {
      pSmg.addValue(pAddresses[i]);
      SMGEdgePointsTo pt =
          new SMGEdgePointsTo(pAddresses[i], pRegions[i], pOffset, SMGTargetSpecifier.REGION);
      pSmg.addPointsToEdge(pt);
      ptEdges[i] = pt;
    }
    return ptEdges;
  }

  private static SMGEdgePointsTo[] addPointersToListsOnHeap(
      CLangSMG pSmg,
      SMGObject[] pLists,
      SMGValue[] pAddresses,
      int pHeadOffset,
      SMGTargetSpecifier pFirstOrLast) {
    if (pLists == null || pAddresses == null || pLists.length == 0) {
      throw new IllegalArgumentException("The provided arrays must not be null or empty.");
    }
    if (pLists.length != pAddresses.length) {
      throw new IllegalArgumentException(
          "The number of lists must be equal to the number of addresses.");
    }
    SMGEdgePointsTo[] ptEdges = new SMGEdgePointsTo[pLists.length];
    for (int i = 0; i < pLists.length; i++) {
      pSmg.addValue(pAddresses[i]);
      SMGEdgePointsTo pt = new SMGEdgePointsTo(pAddresses[i], pLists[i], pHeadOffset, pFirstOrLast);
      pSmg.addPointsToEdge(pt);
      ptEdges[i] = pt;
    }
    return ptEdges;
  }

  // in contrast to linked regions( and linked sll's), linked dll's need two addresses each
  public static SMGValue[] linkObjectsOnHeap(
      CLangSMG pSmg,
      SMGValue[] pAddresses,
      int pHfo,
      int pNfo,
      int pPfo,
      SMGListCircularity pCircularity,
      SMGListLinkage pLinkage) {
    if (pSmg == null) {
      throw new IllegalArgumentException("The smg was null.");
    }
    if (pAddresses == null || pAddresses.length < 1) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    if (pCircularity != SMGListCircularity.OPEN && pCircularity != SMGListCircularity.CIRCULAR) {
      throw new IllegalArgumentException("Unsupported list circularity: " + pCircularity);
    }
    if (pLinkage != SMGListLinkage.SINGLE_LINKED && pLinkage != SMGListLinkage.DOUBLY_LINKED) {
      throw new IllegalArgumentException("Unsupported list linkage: " + pLinkage);
    }

    // to prevent ambiguity, existing links must be deleted before the new linking
    deleteLinksOfObjects(pSmg, pAddresses, pNfo, pPfo);

    CType ptrType = pSmg.getMachineModel().getPointerEquivalentSimpleType();
    final SMGValue firstAddress = pAddresses[0];
    if (!pSmg.isPointer(firstAddress)) {
      throw new IllegalArgumentException(
          "Address " + firstAddress + " is not a valid pointer in the smg.");
    }
    final SMGObject firstNode = pSmg.getObjectPointedBy(firstAddress);

    SMGObject node = null;
    SMGValue address = firstAddress;

    for (int i = 0; i < pAddresses.length; i++) {
      SMGValue previousAddress = address;
      address = pAddresses[i];
      SMGObject previousNode = node;
      if (!pSmg.isPointer(address)) {
        throw new IllegalArgumentException(
            "Address " + address + " is not a valid pointer in the smg.");
      }
      node = pSmg.getObjectPointedBy(address);
      SMGEdgeHasValue hvPrev;
      SMGEdgeHasValue previousHvNext;
      if (i == 0) {
        if (pCircularity == SMGListCircularity.OPEN && pLinkage == SMGListLinkage.DOUBLY_LINKED) {
          hvPrev = new SMGEdgeHasValue(ptrType, pPfo, node, SMGZeroValue.INSTANCE);
          pSmg.addHasValueEdge(hvPrev);
        }
      } else {
        if (pLinkage == SMGListLinkage.DOUBLY_LINKED) {
          if (previousNode.getKind() == SMGObjectKind.DLL) {
            SMGValue address2 = null;
            Set<SMGEdgePointsTo> pte =
                pSmg.getPtEdges(
                    SMGEdgePointsToFilter.targetObjectFilter(previousNode)
                        .filterByTargetSpecifier(SMGTargetSpecifier.LAST));
            if (pte.isEmpty()) {
              address2 = newVal();
              pSmg.addValue(address2);
              SMGEdgePointsTo pt2 =
                  new SMGEdgePointsTo(address2, previousNode, pHfo, SMGTargetSpecifier.LAST);
              pSmg.addPointsToEdge(pt2);
            } else {
              address2 = Iterables.getOnlyElement(pte).getValue();
            }
            hvPrev = new SMGEdgeHasValue(ptrType, pPfo, node, address2);
            pSmg.addHasValueEdge(hvPrev);
          } else {
            hvPrev = new SMGEdgeHasValue(ptrType, pPfo, node, previousAddress);
            pSmg.addHasValueEdge(hvPrev);
          }
        }
        previousHvNext = new SMGEdgeHasValue(ptrType, pNfo, previousNode, address);
        pSmg.addHasValueEdge(previousHvNext);
      }
    }
    // circular lists: connect first and last nodes; else point to NULL
    SMGEdgeHasValue hvNext;
    SMGEdgeHasValue hvPrev;
    if (pCircularity.equals(SMGListCircularity.CIRCULAR)) {
      hvNext = new SMGEdgeHasValue(ptrType, pNfo, node, firstAddress);
      if (pLinkage == SMGListLinkage.DOUBLY_LINKED) {
        if (node.getKind() == SMGObjectKind.DLL) {
          SMGValue address2 = null;
          Set<SMGEdgePointsTo> pte =
              pSmg.getPtEdges(
                  SMGEdgePointsToFilter.targetObjectFilter(node)
                      .filterByTargetSpecifier(SMGTargetSpecifier.LAST));
          if (pte.isEmpty()) {
            address2 = newVal();
            pSmg.addValue(address2);
            SMGEdgePointsTo pt2 =
                new SMGEdgePointsTo(address2, node, pHfo, SMGTargetSpecifier.LAST);
            pSmg.addPointsToEdge(pt2);
          } else {
            address2 = Iterables.getOnlyElement(pte).getValue();
          }
          hvPrev = new SMGEdgeHasValue(ptrType, pPfo, firstNode, address2);
          pSmg.addHasValueEdge(hvPrev);
        } else {
          hvPrev = new SMGEdgeHasValue(ptrType, pPfo, firstNode, address);
          pSmg.addHasValueEdge(hvPrev);
        }
      }
    } else {
      hvNext = new SMGEdgeHasValue(ptrType, pNfo, node, SMGZeroValue.INSTANCE);
    }
    pSmg.addHasValueEdge(hvNext);

    return pAddresses;
  }

  private static SMGValue[] joinValuesPerList(SMGValue[][] pLists) {
    if (pLists == null || pLists.length == 0) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    SMGValue[] values = new SMGValue[pLists.length];
    for (int i = 0; i < pLists.length; i++) {
      SMGValue[] list = pLists[i];
      if (list == null) {
        throw new IllegalArgumentException("The provided array must not contain null.");
      } else if (list.length < 1 || Stream.of(list).distinct().count() > 1) {
        values[i] = newVal();
      } else {
        values[i] = list[0];
      }
    }
    return values;
  }

  private static Integer[] getMinLengths(SMGValue[][] pLists) {
    if (pLists == null || pLists.length == 0) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    Integer[] minLengths = new Integer[pLists.length];
    for (int i = 0; i < pLists.length; i++) {
      if (pLists[i] == null) {
        throw new IllegalArgumentException("The provided array must not contain null.");
      }
      minLengths[i] = pLists[i].length;
    }
    return minLengths;
  }

  private static void deleteLinksOfObjects(
      CLangSMG pSmg, SMGValue[] pAddresses, int pNfo, int pPfo) {
    if (pAddresses == null || pAddresses.length == 0) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    for (SMGValue address : pAddresses) {
      SMGObject object = pSmg.getObjectPointedBy(address);
      for (Integer offset : new Integer[] {pNfo, pPfo}) {
        Set<SMGEdgeHasValue> set =
            pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(object).filterAtOffset(offset));
        for (SMGEdgeHasValue hv : set) {
          pSmg.removeHasValueEdge(hv);
        }
      }
    }
  }

  public static SMGValue[] addLinkedRegionsWithValuesToHeap(
      CLangSMG pSmg,
      SMGValue[] pValues,
      int pSize,
      int pHfo,
      int pNfo,
      int pPfo,
      int pDfo,
      int pDataSize,
      SMGListCircularity pCircularity,
      SMGListLinkage pLinkage) {
    if (pValues == null || pValues.length < 1) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    SMGValue[] addresses = generateNewAddresses(pValues.length);
    SMGObject[] regions = createRegionsOnHeap(pSmg, pValues.length, pSize);
    addPointersToRegionsOnHeap(pSmg, regions, addresses, 0);
    addFieldsToObjectsOnHeap(pSmg, regions, pValues, pDataSize, pDfo);
    return linkObjectsOnHeap(pSmg, addresses, pHfo, pNfo, pPfo, pCircularity, pLinkage);
  }

  public static SMGValue[] addLinkedRegionsWithRegionsWithValuesToHeap(
      CLangSMG pSmg,
      SMGValue[] pValues,
      int pRegionSize,
      int pSubregionSize,
      int pHfo,
      int pNfo,
      int pPfo,
      int pDfo,
      int pSubDfo,
      int pDataSize,
      int pSubDataSize,
      SMGListCircularity pCircularity,
      SMGListLinkage pLinkage) {
    if (pValues == null || pValues.length < 1) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    SMGValue[] addresses = generateNewAddresses(pValues.length);
    SMGObject[] regions = createRegionsOnHeap(pSmg, pValues.length, pRegionSize);
    SMGValue[] subaddresses = generateNewAddresses(pValues.length);
    SMGObject[] subregions = createRegionsOnHeap(pSmg, pValues.length, pSubregionSize);
    addPointersToRegionsOnHeap(pSmg, regions, addresses, 0);
    addPointersToRegionsOnHeap(pSmg, subregions, subaddresses, 0);
    addFieldsToObjectsOnHeap(pSmg, regions, subaddresses, pDataSize, pDfo);
    addFieldsToObjectsOnHeap(pSmg, subregions, pValues, pSubDataSize, pSubDfo);
    return linkObjectsOnHeap(pSmg, addresses, pHfo, pNfo, pPfo, pCircularity, pLinkage);
  }

  public static SMGValue[] addLinkedRegionsWithSublistsWithValuesToHeap(
      CLangSMG pSmg,
      SMGValue[][] pSublists,
      int pNodeSize,
      int pHfo,
      int pNfo,
      int pPfo,
      int pDfo,
      int pDataSize,
      SMGListCircularity pCircularity,
      SMGListLinkage pLinkage) {
    if (pSublists == null || pSublists.length < 1) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    SMGValue[] addresses = generateNewAddresses(pSublists.length);
    SMGObject[] regions = createRegionsOnHeap(pSmg, pSublists.length, pNodeSize);
    SMGValue[] subaddresses = generateNewAddresses(pSublists.length);
    Integer[] minLengths = getMinLengths(pSublists);
    int level = 0;
    SMGObject[] sublists =
        createListsOnHeap(
            pSmg, pSublists.length, pNodeSize, pHfo, pNfo, pPfo, minLengths, level, pLinkage);
    addPointersToRegionsOnHeap(pSmg, regions, addresses, 0);
    addPointersToListsOnHeap(pSmg, sublists, subaddresses, pHfo, SMGTargetSpecifier.FIRST);
    addFieldsToObjectsOnHeap(pSmg, regions, subaddresses, pDataSize, pDfo);
    SMGValue[] values = joinValuesPerList(pSublists);
    addFieldsToObjectsOnHeap(pSmg, sublists, values, pDataSize, pDfo);
    return linkObjectsOnHeap(pSmg, addresses, pHfo, pNfo, pPfo, pCircularity, pLinkage);
  }

  public static SMGValue[] addLinkedListsWithValuesToHeap(
      CLangSMG pSmg,
      SMGValue[][] pLists,
      int pNodeSize,
      int pHfo,
      int pNfo,
      int pPfo,
      int pDfo,
      int pDataSize,
      SMGListCircularity pCircularity,
      SMGListLinkage pLinkage) {
    if (pLists == null || pLists.length == 0) {
      throw new IllegalArgumentException("The provided array must not be null or empty.");
    }
    SMGValue[] addresses = generateNewAddresses(pLists.length);
    int level = 0;
    Integer[] minLengths = getMinLengths(pLists);
    SMGObject[] lists =
        createListsOnHeap(
            pSmg, pLists.length, pNodeSize, pHfo, pNfo, pPfo, minLengths, level, pLinkage);
    addPointersToListsOnHeap(pSmg, lists, addresses, pHfo, SMGTargetSpecifier.FIRST);
    SMGValue[] values = joinValuesPerList(pLists);
    addFieldsToObjectsOnHeap(pSmg, lists, values, pDataSize, pDfo);
    return linkObjectsOnHeap(pSmg, addresses, pHfo, pNfo, pPfo, pCircularity, pLinkage);
  }

  public static SMGRegion addGlobalListPointerToSMG(
      CLangSMG pSmg, SMGValue pHeadAddress, String pLabel) {
    SMGRegion globalVar = new SMGRegion(8 * pSmg.getMachineModel().getSizeofPtr(), pLabel);
    SMGEdgeHasValue hv =
        new SMGEdgeHasValue(
            pSmg.getMachineModel().getPointerEquivalentSimpleType(), 0, globalVar, pHeadAddress);
    pSmg.addGlobalObject(globalVar);
    pSmg.addHasValueEdge(hv);
    return globalVar;
  }

  public static void executeHeapAbstractionWithConsistencyChecks(SMGState pState, CLangSMG pSmg)
      throws SMGInconsistentException {
    SMGAbstractionManager manager =
        new SMGAbstractionManager(LogManager.createTestLogManager(), pSmg, pState);
    pState.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    manager.execute();
    pState.performConsistencyCheck(SMGRuntimeCheck.FORCED);
  }

  public static void assertAbstractListSegmentAsExpected(
      SMGObject pSegment, int pRegionSize, int pLevel, SMGObjectKind pListKind, int pListLength) {
    assertThat(pSegment.isAbstract()).isTrue();
    Truth.assertThat(pSegment.getSize()).isEqualTo(pRegionSize);
    Truth.assertThat(pSegment.getLevel()).isEqualTo(pLevel);
    Truth.assertThat(pSegment.getKind()).isSameInstanceAs(pListKind);
    Truth.assertThat(pSegment).isInstanceOf(SMGAbstractList.class);
    SMGAbstractList<?> segmentAsList = (SMGAbstractList<?>) pSegment;
    Truth.assertThat(segmentAsList.getMinimumLength()).isAtMost(pListLength);
    Truth.assertThat(segmentAsList.getMinimumLength()).isEqualTo(pListLength);
  }

  public static void assertStoredDataOfAbstractList(
      CLangSMG pSmg, SMGValue[] pValues, SMGObject pObject, int pDfo) {
    boolean allValuesEqual = Arrays.stream(pValues).distinct().count() == 1;
    if (allValuesEqual) {
      SMGEdgeHasValue hv =
          Iterables.getOnlyElement(
              pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject).filterAtOffset(pDfo)));
      if (pValues.length > 0) {
        Truth.assertThat(hv.getValue()).isEqualTo(pValues[0]);
      }
    }
  }

  public static void assertStoredDataOfAbstractSublist(
      CLangSMG pSmg, SMGValue[][] pSublists, SMGObject pSubobject, int pDfo) {
    boolean onlyNonEmptySublists = Stream.of(pSublists).noneMatch(e -> e == null || e.length == 0);
    boolean allValuesEqualInUnionOfSublists =
        Stream.of(pSublists).flatMap(Arrays::stream).distinct().count() == 1;
    if (onlyNonEmptySublists && allValuesEqualInUnionOfSublists) {
      SMGEdgeHasValue hv =
          Iterables.getOnlyElement(
              pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSubobject).filterAtOffset(pDfo)));
      Truth.assertThat(hv.getValue()).isEqualTo(pSublists[0][0]);
    }
  }

  private SMGListAbstractionTestHelpers() {}
}
