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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGLevelMapping.SMGJoinLevel;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


final class SMGJoinSubSMGs {
  static private boolean performChecks = false;
  static public void performChecks(boolean pValue) {
    performChecks = pValue;
  }

  private SMGJoinStatus status;
  private boolean defined = false;

  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;

  private SMGNodeMapping mapping1 = null;
  private SMGNodeMapping mapping2 = null;
  private final List<SMGGenericAbstractionCandidate> subSmgAbstractionCandidates;

  public SMGJoinSubSMGs(SMGJoinStatus initialStatus,
      SMG pSMG1, SMG pSMG2, SMG pDestSMG,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      SMGLevelMapping pLevelMap,
      SMGObject pObj1, SMGObject pObj2, SMGObject pNewObject,
      int pLDiff, boolean identicalInputSmg, SMGState pSmgState1, SMGState pSmgState2) throws SMGInconsistentException {

    SMGJoinFields joinFields = new SMGJoinFields(pSMG1, pSMG2, pObj1, pObj2);

    subSmgAbstractionCandidates = ImmutableList.of();
    inputSMG1 = joinFields.getSMG1();
    inputSMG2 = joinFields.getSMG2();

    if (SMGJoinSubSMGs.performChecks) {
      SMGJoinFields.checkResultConsistency(inputSMG1, inputSMG2, pObj1, pObj2);
    }

    destSMG = pDestSMG;
    status = SMGJoinStatus.updateStatus(initialStatus, joinFields.getStatus());
    mapping1 = pMapping1;
    mapping2 = pMapping2;

    /*
     * After joinFields, the objects have identical set of fields. Therefore, to iterate
     * over them, it is sufficient to loop over HV set in the first SMG, and just
     * obtain the (always just single one) corresponding edge from the second
     * SMG.
     */

    SMGEdgeHasValueFilter filterOnSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilter filterOnSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);

    Set<SMGEdgeHasValue> edgesOnObject1 = Sets.newHashSet(inputSMG1.getHVEdges(filterOnSMG1));

    Map<Integer, List<SMGGenericAbstractionCandidate>> valueAbstractionCandidates = new HashMap<>();
    boolean allValuesDefined = true;

    int prevLevel = pLevelMap.get(SMGJoinLevel.valueOf(pObj1.getLevel(), pObj2.getLevel()));

    for (SMGEdgeHasValue hvIn1 : edgesOnObject1) {
      filterOnSMG2.filterAtOffset(hvIn1.getOffset());

      int lDiff = pLDiff;

      SMGEdgeHasValue hvIn2 = Iterables.getOnlyElement(inputSMG2.getHVEdges(filterOnSMG2));

      int value1Level = getValueLevel(pObj1, hvIn1.getValue(), inputSMG1);
      int value2Level =
          getValueLevel(pObj2, hvIn2.getValue(), inputSMG2);

      int levelDiff1 = value1Level - pObj1.getLevel();
      int levelDiff2 = value2Level - pObj2.getLevel();

      lDiff = lDiff + (levelDiff1 - levelDiff2);

      SMGLevelMapping levelMap =
          updateLevelMap(value1Level, value2Level, pLevelMap, pObj1.getLevel(), pObj2.getLevel());

      if (levelMap == null) {
        defined = false;
        return;
      }

      SMGJoinValues joinValues = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG,
          mapping1, mapping2, levelMap, hvIn1.getValue(), hvIn2.getValue(), lDiff, identicalInputSmg, value1Level, value2Level, prevLevel, pSmgState1, pSmgState2);

      /* If the join of the values is not defined and can't be
       * recovered through abstraction, the join fails.*/
      if (!joinValues.isDefined() && !joinValues.isRecoverable()) {
        //subSmgAbstractionCandidates = ImmutableList.of();
        return;
      }

      status = joinValues.getStatus();
      inputSMG1 = joinValues.getInputSMG1();
      inputSMG2 = joinValues.getInputSMG2();
      destSMG = joinValues.getDestinationSMG();
      mapping1 = joinValues.getMapping1();
      mapping2 = joinValues.getMapping2();

      if (joinValues.isDefined()) {

        SMGEdgeHasValue newHV;

        if (hvIn1.getObject().equals(pNewObject)
            && joinValues.getValue().equals(hvIn1.getValue())) {
          newHV = hvIn1;
        } else {
          newHV = new SMGEdgeHasValue(hvIn1.getType(), hvIn1.getOffset(), pNewObject,
              joinValues.getValue());
        }

        destSMG.addHasValueEdge(newHV);

        if(joinValues.subSmgHasAbstractionsCandidates()) {
          valueAbstractionCandidates.put(joinValues.getValue(), joinValues.getAbstractionCandidates());
        }
      } else {
        allValuesDefined = false;
      }
    }

    /* If the join is defined without abstraction candidates in
       sub smgs, we don't need to perform abstraction.*/
    if (allValuesDefined && valueAbstractionCandidates.isEmpty()) {
      defined = true;
      //subSmgAbstractionCandidates = ImmutableList.of();
      return;
    }

    //SMGJoinAbstractionManager abstractionManager = new SMGJoinAbstractionManager(pObj1, pObj2, inputSMG1, inputSMG2, pNewObject, destSMG);
    //subSmgAbstractionCandidates = abstractionManager.calculateCandidates(valueAbstractionCandidates);

    /*If abstraction candidates can be found for this sub Smg, then the join for this sub smg
     *  is defined under the assumption, that the abstraction of one abstraction candidate is executed.*/
    //if (!subSmgAbstractionCandidates.isEmpty()) {
      //defined = true;
      //return;
    //}

    /* If no abstraction can be found for this sub Smg, then the join is only defined,
     * if all values are defined. For values that are defined under the assumption,
     * that a abstraction candidate is execued for the destination smg, execute the abstraction
     * so that the join of this sub SMG is complete.*/
    if(!allValuesDefined) {
      defined = false;
      return;
    }

    for(List<SMGGenericAbstractionCandidate> abstractionCandidates : valueAbstractionCandidates.values()) {
      abstractionCandidates.iterator().next().execute(destSMG);
    }

    defined = true;
  }

  private SMGLevelMapping updateLevelMap(int pValue1Level, int pValue2Level,
      SMGLevelMapping pLevelMap, int pObjectLevel, int pObjectLevel2) {

    SMGJoinLevel joinLevel = SMGJoinLevel.valueOf(pValue1Level, pValue2Level);

    if (pLevelMap.containsKey(joinLevel)) {
      return pLevelMap;
    }

    int oldLevel = pLevelMap.get(SMGJoinLevel.valueOf(pObjectLevel, pObjectLevel2));

    if (pValue1Level == pObjectLevel + 1 || pValue2Level == pObjectLevel2 + 1) {
      int result = oldLevel + 1;
      SMGLevelMapping newLevelMap = new SMGLevelMapping();
      newLevelMap.putAll(pLevelMap);
      newLevelMap.put(joinLevel, result);
      return newLevelMap;
    }

    if (pValue1Level == pObjectLevel && pValue2Level == pObjectLevel) {
      pLevelMap.put(joinLevel, oldLevel);
      SMGLevelMapping newLevelMap = new SMGLevelMapping();
      newLevelMap.putAll(pLevelMap);
      newLevelMap.put(joinLevel, oldLevel);
      return newLevelMap;
    }

    if (pValue1Level == pObjectLevel - 1 && pValue2Level == pObjectLevel2 - 1) {
      int result = oldLevel - 1;
      SMGLevelMapping newLevelMap = new SMGLevelMapping();
      newLevelMap.putAll(pLevelMap);
      newLevelMap.put(joinLevel, result);
      return newLevelMap;
    }

    return null;
  }

  private int getValueLevel(SMGObject pObject, int pValue, SMG pInputSMG1) {

    if (pInputSMG1.isPointer(pValue)) {
      SMGEdgePointsTo pointer = pInputSMG1.getPointer(pValue);

      if (pointer.getTargetSpecifier() == SMGTargetSpecifier.ALL) {
        return pObject.getLevel() + 1;
      } else {
        SMGObject targetObject = pointer.getObject();
        return targetObject.getLevel();
      }
    } else {
      return 0;
    }
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getSMG1() {
    return inputSMG1;
  }

  public SMG getSMG2() {
    return inputSMG2;
  }

  public SMG getDestSMG() {
    return destSMG;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public List<SMGGenericAbstractionCandidate> getSubSmgAbstractionCandidates() {
    return subSmgAbstractionCandidates;
  }
}