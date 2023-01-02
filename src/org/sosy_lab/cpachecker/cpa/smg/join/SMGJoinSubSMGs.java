// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGGenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

final class SMGJoinSubSMGs {
  private static boolean performChecks = false;

  private SMGJoinStatus status;
  private boolean defined = false;

  private UnmodifiableSMG inputSMG1;
  private UnmodifiableSMG inputSMG2;
  private SMG destSMG;

  private final SMGNodeMapping mapping1;
  private final SMGNodeMapping mapping2;
  private final List<SMGGenericAbstractionCandidate> subSmgAbstractionCandidates;

  /** Algorithm 4 from FIT-TR-2012-04. */
  public SMGJoinSubSMGs(
      SMGJoinStatus initialStatus,
      UnmodifiableSMG pSMG1,
      UnmodifiableSMG pSMG2,
      SMG pDestSMG,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      SMGLevelMapping pLevelMap,
      SMGObject pObj1,
      SMGObject pObj2,
      SMGObject pNewObject,
      int pLDiff,
      boolean identicalInputSmg,
      UnmodifiableSMGState pSmgState1,
      UnmodifiableSMGState pSmgState2)
      throws SMGInconsistentException {

    // Algorithm 4 from FIT-TR-2012-04, line 1
    SMGJoinFields joinFields = new SMGJoinFields(pSMG1, pSMG2, pObj1, pObj2);
    inputSMG1 = joinFields.getSMG1();
    inputSMG2 = joinFields.getSMG2();
    status = initialStatus.updateWith(joinFields.getStatus());
    subSmgAbstractionCandidates = ImmutableList.of();
    destSMG = pDestSMG;
    mapping1 = pMapping1;
    mapping2 = pMapping2;

    if (SMGJoinSubSMGs.performChecks) {
      SMGJoinFields.checkResultConsistency(inputSMG1, inputSMG2, pObj1, pObj2);
    }

    /*
     * After joinFields, the objects have identical set of fields. Therefore, to iterate
     * over them, it is sufficient to loop over HV set in the first SMG, and just
     * obtain the (always just single one) corresponding edge from the second
     * SMG.
     */

    // Algorithm 4 from FIT-TR-2012-04, line 2 and 3 interleaved
    // TODO seems to be buggy, does not fully match the algorithm from TR.
    SMGEdgeHasValueFilterByObject filterOnSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilterByObject filterOnSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    Map<SMGValue, List<SMGGenericAbstractionCandidate>> valueAbstractionCandidates =
        new HashMap<>();
    boolean allValuesDefined = true;

    int prevLevel = pLevelMap.get(SMGJoinLevel.valueOf(pObj1.getLevel(), pObj2.getLevel()));

    SMGHasValueEdges hvEdgesIn1 = inputSMG1.getHVEdges(filterOnSMG1);
    SMGHasValueEdges hvEdgesIn2 = inputSMG2.getHVEdges(filterOnSMG2);
    boolean edgesAreAdded = false;
    if (!hvEdgesIn1.isEmpty() && hvEdgesIn1.equals(hvEdgesIn2) && pObj1.equals(pNewObject)) {
      // Fast copy edges
      destSMG.addHasValueEdges(hvEdgesIn1);
      edgesAreAdded = true;
    }

    for (SMGEdgeHasValue hvIn1 : hvEdgesIn1) {
      Iterable<SMGEdgeHasValue> hvEdges2 =
          hvEdgesIn2.filter(
              new SMGEdgeHasValueFilter().filterAtOffset(hvIn1.getOffset()).filterWithoutSize());

      // FIXME: check why it is possible to have no edges
      if (Iterables.size(hvEdges2) != 1) {
        defined = false;
        status = SMGJoinStatus.INCOMPARABLE;
        return;
      }

      SMGEdgeHasValue hvIn2 = Iterables.getOnlyElement(hvEdges2);

      int value1Level = getValueLevel(pObj1, hvIn1.getValue(), inputSMG1);
      int value2Level = getValueLevel(pObj2, hvIn2.getValue(), inputSMG2);
      int levelDiff1 = value1Level - pObj1.getLevel();
      int levelDiff2 = value2Level - pObj2.getLevel();
      int lDiff = pLDiff + (levelDiff1 - levelDiff2);

      SMGLevelMapping levelMap =
          updateLevelMap(value1Level, value2Level, pLevelMap, pObj1.getLevel(), pObj2.getLevel());

      if (levelMap == null) {
        defined = false;
        status = SMGJoinStatus.INCOMPARABLE;
        return;
      }

      SMGJoinValues joinValues =
          new SMGJoinValues(
              status,
              inputSMG1,
              inputSMG2,
              destSMG,
              mapping1,
              mapping2,
              levelMap,
              hvIn1.getValue(),
              hvIn2.getValue(),
              lDiff,
              identicalInputSmg,
              value1Level,
              value2Level,
              prevLevel,
              pSmgState1,
              pSmgState2);
      status = joinValues.getStatus();

      /* If the join of the values is not defined and can't be
       * recovered through abstraction, the join fails.*/
      if (!joinValues.isDefined() && !joinValues.isRecoverable()) {
        // subSmgAbstractionCandidates = ImmutableList.of();
        status = SMGJoinStatus.INCOMPARABLE;
        return;
      }

      inputSMG1 = joinValues.getInputSMG1();
      inputSMG2 = joinValues.getInputSMG2();
      destSMG = joinValues.getDestinationSMG();

      if (joinValues.isDefined()) {

        if (hvIn1.getObject().equals(pNewObject)
            && joinValues.getValue().equals(hvIn1.getValue())) {
          if (!edgesAreAdded) {
            destSMG.addHasValueEdge(hvIn1);
          }
        } else {
          SMGEdgeHasValue newHV =
              new SMGEdgeHasValue(
                  hvIn1.getSizeInBits(), hvIn1.getOffset(), pNewObject, joinValues.getValue());
          if (edgesAreAdded) {
            destSMG.removeHasValueEdge(hvIn1);
          }
          destSMG.addHasValueEdge(newHV);
        }

        if (joinValues.subSmgHasAbstractionsCandidates()) {
          valueAbstractionCandidates.put(
              joinValues.getValue(), joinValues.getAbstractionCandidates());
        }
      } else {
        allValuesDefined = false;
      }
    }

    /* If the join is defined without abstraction candidates in
    sub smgs, we don't need to perform abstraction.*/
    if (allValuesDefined && valueAbstractionCandidates.isEmpty()) {
      defined = true;
      // subSmgAbstractionCandidates = ImmutableList.of();
      return;
    }

    // SMGJoinAbstractionManager abstractionManager = new SMGJoinAbstractionManager(pObj1, pObj2,
    // inputSMG1, inputSMG2, pNewObject, destSMG);
    // subSmgAbstractionCandidates =
    // abstractionManager.calculateCandidates(valueAbstractionCandidates);

    /*If abstraction candidates can be found for this sub Smg, then the join for this sub smg
     *  is defined under the assumption, that the abstraction of one abstraction candidate is executed.*/
    // if (!subSmgAbstractionCandidates.isEmpty()) {
    // defined = true;
    // return;
    // }

    /* If no abstraction can be found for this sub Smg, then the join is only defined,
     * if all values are defined. For values that are defined under the assumption,
     * that a abstraction candidate is execued for the destination smg, execute the abstraction
     * so that the join of this sub SMG is complete.*/
    if (!allValuesDefined) {
      status = SMGJoinStatus.INCOMPARABLE;
      defined = false;
      return;
    }

    for (List<SMGGenericAbstractionCandidate> abstractionCandidates :
        valueAbstractionCandidates.values()) {
      abstractionCandidates.iterator().next().execute(destSMG);
    }

    defined = true;
  }

  private SMGLevelMapping updateLevelMap(
      int pValue1Level,
      int pValue2Level,
      SMGLevelMapping pLevelMap,
      int pObjectLevel,
      int pObjectLevel2) {

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

  private int getValueLevel(SMGObject pObject, SMGValue pValue, UnmodifiableSMG pInputSMG1) {

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
    if (!defined) {
      checkState(
          status == SMGJoinStatus.INCOMPARABLE,
          "Join of SubSMGs not defined, but status is %s",
          status);
    }
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public UnmodifiableSMG getSMG1() {
    return inputSMG1;
  }

  public UnmodifiableSMG getSMG2() {
    return inputSMG2;
  }

  public UnmodifiableSMG getDestSMG() {
    return destSMG;
  }

  public List<SMGGenericAbstractionCandidate> getSubSmgAbstractionCandidates() {
    return subSmgAbstractionCandidates;
  }
}
