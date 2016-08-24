/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;

import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGNodeMapping;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedList;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class SMGIntersectStates {

  private SMGIntersectStates() {}

  public static SMGIntersectionResult intersect(SMGState pSmgState1, CLangSMG pHeap1, SMGState pSmgState2,
      CLangSMG pHeap2, BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2) {

    CLangSMG destSMG = new CLangSMG(pHeap1.getMachineModel());

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();
    mapping1.map(pSmgState1.getNullObject(), destSMG.getNullObject());
    mapping2.map(pSmgState2.getNullObject(), destSMG.getNullObject());

    Map<String, SMGRegion> globals_in_smg1 = pHeap1.getGlobalObjects();
    Deque<CLangStackFrame> stack_in_smg1 = pHeap1.getStackFrames();
    Map<String, SMGRegion> globals_in_smg2 = pHeap2.getGlobalObjects();
    Deque<CLangStackFrame> stack_in_smg2 = pHeap2.getStackFrames();

    Set<SMGEdgeHasValue> singleHveEdge1 = new HashSet<>();
    Set<SMGEdgeHasValue> singleHveEdge2 = new HashSet<>();

    BiMap<SMGKnownSymValue, SMGKnownExpValue> destExplicitValues = HashBiMap.create();

    Set<String> globalVars = new HashSet<>();
    globalVars.addAll(globals_in_smg1.keySet());
    globalVars.addAll(globals_in_smg2.keySet());

    for (String globalVar : globalVars) {
      SMGRegion globalInSMG1 = globals_in_smg1.get(globalVar);
      SMGRegion globalInSMG2 = globals_in_smg2.get(globalVar);

      SMGRegion finalObject = globalInSMG1;
      destSMG.addGlobalObject(finalObject);
      mapping1.map(globalInSMG1, finalObject);
      mapping2.map(globalInSMG2, finalObject);
    }

    Iterator<CLangStackFrame> smg1stackIterator = stack_in_smg1.descendingIterator();
    Iterator<CLangStackFrame> smg2stackIterator = stack_in_smg2.descendingIterator();

    while ( smg1stackIterator.hasNext() && smg2stackIterator.hasNext() ) {
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      destSMG.addStackFrame(frameInSMG1.getFunctionDeclaration());

      Set<String> localVars = new HashSet<>();
      localVars.addAll(frameInSMG1.getVariables().keySet());
      localVars.addAll(frameInSMG2.getVariables().keySet());

      for (String localVar : localVars) {
        SMGRegion localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGRegion localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGRegion finalObject = localInSMG1;
        destSMG.addStackObject(finalObject);
        mapping1.map(localInSMG1, finalObject);
        mapping2.map(localInSMG2, finalObject);
      }

      SMGObject returnSMG1 = frameInSMG1.getReturnObject();
      SMGObject returnSMG2 = frameInSMG2.getReturnObject();

      if (returnSMG1 == null) {
        continue;
      }

      SMGObject finalObject = destSMG.getFunctionReturnObject();
      mapping1.map(returnSMG1, finalObject);
      mapping2.map(returnSMG2, finalObject);
    }

    for (String globalVar : globalVars) {
      SMGRegion globalInSMG1 = globals_in_smg1.get(globalVar);
      SMGRegion globalInSMG2 = globals_in_smg2.get(globalVar);

      SMGObject finalObject = mapping1.get(globalInSMG1);
      boolean defined = intersectPairFields(pHeap1, pHeap2, globalInSMG1, globalInSMG2, finalObject, mapping1, mapping2, destSMG, singleHveEdge1, singleHveEdge2, pExplicitValues, pExplicitValues2, destExplicitValues);

      if (!defined) {
        return SMGIntersectionResult.getNotDefinedInstance();
      }
    }

    smg1stackIterator = stack_in_smg1.descendingIterator();
    smg2stackIterator = stack_in_smg2.descendingIterator();

    while ( smg1stackIterator.hasNext() && smg2stackIterator.hasNext() ) {
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      Set<String> localVars = new HashSet<>();
      localVars.addAll(frameInSMG1.getVariables().keySet());
      localVars.addAll(frameInSMG2.getVariables().keySet());

      for (String localVar : localVars) {
        SMGRegion localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGRegion localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGObject finalObject = mapping1.get(localInSMG1);

        boolean defined = intersectPairFields(pHeap1, pHeap2, localInSMG1, localInSMG2, finalObject,
            mapping1, mapping2, destSMG, singleHveEdge1, singleHveEdge2, pExplicitValues,
            pExplicitValues2, destExplicitValues);

        if (!defined) {
          return SMGIntersectionResult.getNotDefinedInstance();
        }
      }

      SMGObject returnSMG1 = frameInSMG1.getReturnObject();
      SMGObject returnSMG2 = frameInSMG2.getReturnObject();

      if (returnSMG1 == null) {
        continue;
      }

      SMGObject finalObject = destSMG.getFunctionReturnObject();

      boolean defined = intersectPairFields(pHeap1, pHeap2, returnSMG1, returnSMG2, finalObject,
          mapping1, mapping2, destSMG, singleHveEdge1, singleHveEdge2, pExplicitValues,
          pExplicitValues2, destExplicitValues);

      if (!defined) {
        return SMGIntersectionResult.getNotDefinedInstance();
      }
    }

    for(SMGEdgeHasValue hve1 : singleHveEdge1) {
      intersectHveEdgeWithTop(hve1, pHeap1, destSMG, pExplicitValues, destExplicitValues, mapping1);
    }

    for(SMGEdgeHasValue hve2 : singleHveEdge2) {
      intersectHveEdgeWithTop(hve2, pHeap2, destSMG, pExplicitValues2, destExplicitValues, mapping2);
    }

    SMGState pIntersectResult = new SMGState(pSmgState1, destSMG, destExplicitValues);

    return new SMGIntersectionResult(pSmgState1, pSmgState2, pIntersectResult, true);
  }

  private static void intersectHveEdgeWithTop(SMGEdgeHasValue pHve, CLangSMG pSmg,
      CLangSMG pDestSMG, BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues, SMGNodeMapping pMapping) {

    SMGObject destObject = pMapping.get(pHve.getObject());
    int value = pHve.getValue();

    intersectValueWithTop(value, pSmg, pDestSMG, pExplicitValues, pDestExplicitValues, pMapping);

    int destValue = pMapping.get(value);
    SMGEdgeHasValue destHve = new SMGEdgeHasValue(pHve.getType(), pHve.getOffset(), destObject, destValue);
    pDestSMG.addHasValueEdge(destHve);
  }

  private static void intersectValueWithTop(int pValue, CLangSMG pSmg, CLangSMG pDestSMG,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues, SMGNodeMapping pMapping) {

    if(pMapping.containsKey(pValue)) {
      return;
    }

    pMapping.map(pValue, pValue);

    SMGKnownSymValue symVal = SMGKnownSymValue.valueOf(pValue);
    if (pExplicitValues.containsKey(symVal)) {
      SMGKnownExpValue pExpVal = pExplicitValues.get(symVal);
      pDestExplicitValues.put(symVal, pExpVal);
    }

    if (pSmg.isPointer(pValue)) {
      SMGEdgePointsTo pte = pSmg.getPointer(pValue);
      intersectPointerWithTop(pte, pSmg, pDestSMG, pExplicitValues, pDestExplicitValues,
          pMapping);
    }
  }

  private static void intersectPointerWithTop(SMGEdgePointsTo pPte, CLangSMG pSmg,
      CLangSMG pDestSMG,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues, SMGNodeMapping pMapping) {

    SMGObject object = pPte.getObject();

    intersectObjectWithTop(object, pSmg, pDestSMG, pExplicitValues, pDestExplicitValues, pMapping);

    pDestSMG.addPointsToEdge(pPte);

  }

  private static void intersectObjectWithTop(SMGObject pObject, CLangSMG pSmg, CLangSMG pDestSMG,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues, SMGNodeMapping pMapping) {

    if(pMapping.containsKey(pObject)) {
      return;
    }

    pDestSMG.addHeapObject(pObject);
    pMapping.map(pObject, pObject);

    Set<SMGEdgeHasValue> hves = pSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    for (SMGEdgeHasValue hve : hves) {
      intersectHveEdgeWithTop(hve, pSmg, pDestSMG, pExplicitValues, pDestExplicitValues, pMapping);
    }
  }

  private static boolean intersectPairFields(CLangSMG pSmg1, CLangSMG pSmg2, SMGObject pObject1,
      SMGObject pObject2, SMGObject pDestObject, SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2, CLangSMG pDestSMG, Set<SMGEdgeHasValue> pSingleHveEdge1,
      Set<SMGEdgeHasValue> pSingleHveEdge2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues) {

    Set<SMGEdgeHasValue> hves1 = pSmg1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject1));
    Set<SMGEdgeHasValue> hves2 = pSmg2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject2));

    Map<Integer, SMGEdgeHasValue> offsetToHve1Map =
        FluentIterable.from(hves1).uniqueIndex(
            (SMGEdgeHasValue hve) -> {
              return hve.getOffset();
            });

    Map<Integer, SMGEdgeHasValue> offsetToHve2Map =
        FluentIterable.from(hves2).uniqueIndex(
            (SMGEdgeHasValue hve) -> {
              return hve.getOffset();
            });

    Set<Integer> offsetSet = new HashSet<>(offsetToHve1Map.size() + offsetToHve2Map.size());
    offsetSet.addAll(offsetToHve1Map.keySet());
    offsetSet.addAll(offsetToHve2Map.keySet());

    for (Integer offset : offsetSet) {
      if (offsetToHve1Map.containsKey(offset)) {
        if (offsetToHve2Map.containsKey(offset)) {
          SMGEdgeHasValue hve1 = offsetToHve1Map.get(offset);
          SMGEdgeHasValue hve2 = offsetToHve2Map.get(offset);
          boolean defined = intersectPairHveEdges(hve1, hve2, pSmg1, pSmg2, pDestSMG, pDestObject,
              pMapping1, pMapping2, pSingleHveEdge1, pSingleHveEdge2, pExplicitValues,
              pExplicitValues2, pDestExplicitValues);

          if (!defined) {
            return false;
          }
        } else {
          SMGEdgeHasValue hve1 = offsetToHve1Map.get(offset);
          pSingleHveEdge1.add(hve1);
        }
      } else {
        SMGEdgeHasValue hve2 = offsetToHve2Map.get(offset);
        pSingleHveEdge2.add(hve2);
      }
    }

    return true;
  }

  private static boolean intersectPairHveEdges(SMGEdgeHasValue pHve1, SMGEdgeHasValue pHve2,
      CLangSMG pSmg1, CLangSMG pSmg2, CLangSMG pDestSMG, SMGObject pDestObject,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, Set<SMGEdgeHasValue> pSingleHveEdge1,
      Set<SMGEdgeHasValue> pSingleHveEdge2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues) {

    int value1 = pHve1.getValue();
    int value2 = pHve2.getValue();

    boolean defined = intersectValues(value1, value2, pSmg1, pSmg2, pDestSMG, pMapping1, pMapping2, pSingleHveEdge1, pSingleHveEdge2, pExplicitValues, pExplicitValues2, pDestExplicitValues);

    if (!defined) {
      return false;
    }

    int destValue = pMapping1.get(value1);

    SMGEdgeHasValue destHveEdge =
        new SMGEdgeHasValue(pHve1.getType(), pHve1.getOffset(), pDestObject, destValue);
    pDestSMG.addHasValueEdge(destHveEdge);

    return true;
  }

  private static boolean intersectValues(int pValue1, int pValue2, CLangSMG pSmg1, CLangSMG pSmg2,
      CLangSMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      Set<SMGEdgeHasValue> pSingleHveEdge1, Set<SMGEdgeHasValue> pSingleHveEdge2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues) {

    boolean containsValue1 = pMapping1.containsKey(pValue1);
    boolean containsValue2 = pMapping2.containsKey(pValue2);

    /*Already intersected*/
    if (containsValue1
        && containsValue2) {
      return pMapping1.get(pValue1).equals(pMapping2.get(pValue2));
    }

    /*Intesect is null due to different values.*/
    if (containsValue1 || containsValue2) {
      return false;
    }

    int destValue = pValue1;

    pMapping1.map(pValue1, destValue);
    pMapping2.map(pValue2, destValue);
    pDestSMG.addValue(destValue);

    boolean isPointer1 = pSmg1.isPointer(pValue1);
    boolean isPointer2 = pSmg2.isPointer(pValue2);

    if ((isPointer1 && !isPointer2) || (!isPointer1 && isPointer2)) {
      return false;
    }

    if (isPointer1 && isPointer2) {
      SMGEdgePointsTo pte1 = pSmg1.getPointer(pValue1);
      SMGEdgePointsTo pte2 = pSmg2.getPointer(pValue2);

      boolean defined = intersectPairPointsToEdges(pte1, pte2, destValue, pSmg1, pSmg2, pDestSMG, pMapping1,
          pMapping2, pSingleHveEdge1, pSingleHveEdge2, pExplicitValues, pExplicitValues2,
          pDestExplicitValues);

      if (!defined) {
        return false;
      }
    }

    SMGKnownSymValue symVal1 = SMGKnownSymValue.valueOf(pValue1);
    SMGKnownSymValue symVal2 = SMGKnownSymValue.valueOf(pValue2);
    SMGKnownSymValue symDestVal = symVal1;

    SMGExplicitValue expVal1 = SMGUnknownValue.getInstance();
    SMGExplicitValue expVal2 = SMGUnknownValue.getInstance();

    if (pExplicitValues.containsKey(symVal1)) {
      expVal1 = pExplicitValues.get(symVal1);
    }

    if (pExplicitValues.containsKey(symVal2)) {
      expVal2 = pExplicitValues.get(symVal2);
    }

    if (!expVal1.isUnknown() && !expVal2.isUnknown()) {
      if (expVal1.equals(expVal2)) {
        pDestExplicitValues.put(symDestVal, (SMGKnownExpValue) expVal1);
      } else {
        return false;
      }
    } else if (!expVal1.isUnknown()) {
      pDestExplicitValues.put(symDestVal, (SMGKnownExpValue) expVal1);
    } else if (!expVal2.isUnknown()) {
      pDestExplicitValues.put(symDestVal, (SMGKnownExpValue) expVal2);
    }

    return true;
  }

  private static boolean intersectPairPointsToEdges(SMGEdgePointsTo pPte1, SMGEdgePointsTo pPte2,
      int destValue, CLangSMG pSmg1, CLangSMG pSmg2,
      CLangSMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      Set<SMGEdgeHasValue> pSingleHveEdge1, Set<SMGEdgeHasValue> pSingleHveEdge2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues) {

    int offset1 = pPte1.getOffset();
    int offset2 = pPte2.getOffset();

    if (offset1 != offset2) {
      return false;
    }

    SMGTargetSpecifier tg1 = pPte1.getTargetSpecifier();
    SMGTargetSpecifier tg2 = pPte2.getTargetSpecifier();

    // TODO Imprecise intersect, handle abstraction correctly
    if (tg1 != tg2) {
      return false;
    }

    SMGObject obj1 = pPte1.getObject();
    SMGObject obj2 = pPte2.getObject();

    boolean defined = intersectObjectPair(obj1, obj2, pSmg1, pSmg2, pDestSMG, pMapping1, pMapping2, pSingleHveEdge1, pSingleHveEdge2, pExplicitValues, pExplicitValues2, pDestExplicitValues);

    if(!defined) {
      return false;
    }

    SMGObject destObject = pMapping1.get(obj1);

    SMGEdgePointsTo destPte = new SMGEdgePointsTo(destValue, destObject, offset1, tg1);
    pDestSMG.addPointsToEdge(destPte);

    return true;
  }

  private static boolean intersectObjectPair(SMGObject pObj1, SMGObject pObj2,
      CLangSMG pSmg1, CLangSMG pSmg2,
      CLangSMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      Set<SMGEdgeHasValue> pSingleHveEdge1, Set<SMGEdgeHasValue> pSingleHveEdge2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pExplicitValues2,
      BiMap<SMGKnownSymValue, SMGKnownExpValue> pDestExplicitValues) {

    boolean containsObject1 = pMapping1.containsKey(pObj1);
    boolean containsObject2 = pMapping2.containsKey(pObj2);

    /*Already intersected*/
    if (containsObject1
        && containsObject2) {
      return pMapping1.get(pObj1).equals(pMapping2.get(pObj2));
    }

    /*Intesect is null due to different pointer.*/
    if (containsObject1 || containsObject2) {
      return false;
    }

    boolean match = matchObject(pObj1, pObj2);

    if (!match) {
      return false;
    }

    SMGObject destObject = getConcretestObject(pObj1, pObj2);

    pMapping1.map(pObj1, destObject);
    pMapping2.map(pObj2, destObject);

    /*Global and stack objects already mapped */
    pDestSMG.addHeapObject(destObject);

    boolean defined =
        intersectPairFields(pSmg1, pSmg2, pObj1, pObj2, destObject, pMapping1, pMapping2, pDestSMG,
            pSingleHveEdge1, pSingleHveEdge2, pExplicitValues, pExplicitValues2,
            pDestExplicitValues);

    return defined;
  }

  private static boolean matchObject(SMGObject pObj1, SMGObject pObj2) {

    if (pObj1.getSize() != pObj2.getSize()) {
      return false;
    }

    if (pObj1.getLevel() != pObj2.getLevel()) {
      return false;
    }

    if (pObj1.getKind() != pObj2.getKind()) {

      switch (pObj1.getKind()) {
        case OPTIONAL:
          switch (pObj2.getKind()) {
            case REG:
            case DLL:
            case SLL:
              return true;
            default:
              return false;
          }
        case REG:
        case DLL:
        case SLL:
          return pObj2.getKind() == SMGObjectKind.OPTIONAL;
        default:
          return false;
      }
    }

    switch (pObj1.getKind()) {
      case DLL:
        return ((SMGDoublyLinkedList) pObj1).matchSpecificShape((SMGDoublyLinkedList) pObj2);
      case SLL:
        return ((SMGSingleLinkedList) pObj1).matchSpecificShape((SMGSingleLinkedList) pObj2);
      case GENERIC:
        //TODO match generic
        return pObj1.equals(pObj2);
      default:
        return true;
    }
  }

  private static SMGObject getConcretestObject(SMGObject pObj1, SMGObject pObj2) {

    /*Determine which object results in the least amount of concrete states
     * if included in a smg state.*/

    if (!pObj1.isAbstract()) {
      return pObj1;
    }

    if (!pObj2.isAbstract()) {
      return pObj2;
    }

    SMGObjectKind kind1 = pObj1.getKind();
    SMGObjectKind kind2 = pObj2.getKind();

    if (kind1 == SMGObjectKind.OPTIONAL) {
      return pObj2;
    }

    if (kind2 == SMGObjectKind.OPTIONAL) {
      return pObj1;
    }

    if (kind1 == kind2) {
      switch (kind1) {
        case DLL:
          int length1 = ((SMGDoublyLinkedList) pObj1).getMinimumLength();
          int length2 = ((SMGDoublyLinkedList) pObj2).getMinimumLength();
          return length1 < length2 ? pObj2 : pObj1;
        case SLL:
          length1 = ((SMGSingleLinkedList) pObj1).getMinimumLength();
          length2 = ((SMGSingleLinkedList) pObj2).getMinimumLength();
          return length1 < length2 ? pObj2 : pObj1;
        default:
          return pObj1;
      }
    }

    return pObj1;
  }

  public static class SMGIntersectionResult {
    private static final SMGIntersectionResult NOT_DEFINED = new SMGIntersectionResult(null, null, null, false);
    private final SMGState smg1;
    private final SMGState smg2;
    private final SMGState combinationResult;
    private final boolean defined;

    public SMGIntersectionResult(SMGState pSmg1, SMGState pSmg2, SMGState pJoinResult,
        boolean pDefined) {
      super();
      smg1 = pSmg1;
      smg2 = pSmg2;
      combinationResult = pJoinResult;
      defined = pDefined;
    }

    public SMGState getSmg1() {
      return smg1;
    }

    public SMGState getSmg2() {
      return smg2;
    }

    public boolean isDefined() {
      return defined;
    }

    public SMGState getCombinationResult() {
      return combinationResult;
    }

    public static SMGIntersectionResult getNotDefinedInstance() {
      return NOT_DEFINED;
    }
  }
}