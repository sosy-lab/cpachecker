// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.5) */
public class SMGInsertLeftDlsAndJoin extends SMGAbstractJoin {

  public SMGInsertLeftDlsAndJoin(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGValue pValue1,
      SMGValue pValue2,
      int pNestingLevelDiff) {
    super(pStatus, pInputSMG1, pInputSMG2, pDestSMG, pMapping1, pMapping2);
    checkPointsToDLLs(pValue1);
    insertLeftDlsAndJoin(pValue1, pValue2, pNestingLevelDiff);
  }

  /**
   * Implementation of Algorithm 9.
   *
   * @param pValue1 - pointer to dlls in first smg
   * @param pValue2 - pointer of second smg to be joined
   * @param pNestingLevelDiff - difference of nesting levels
   */
  private void insertLeftDlsAndJoin(SMGValue pValue1, SMGValue pValue2, int pNestingLevelDiff) {
    // step 1
    Optional<SMGPointsToEdge> edgeOptionalV1 = inputSMG1.getPTEdge(pValue1);
    checkArgument(edgeOptionalV1.isPresent());

    SMGPointsToEdge pToEdge1 = edgeOptionalV1.orElseThrow();
    // safe cast, ensured by sanity check
    SMGDoublyLinkedListSegment dlls1 = (SMGDoublyLinkedListSegment) pToEdge1.pointsTo();
    BigInteger nextFieldOffset;
    if (pToEdge1.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
      nextFieldOffset = dlls1.getNextOffset();
    } else if (pToEdge1.targetSpecifier().equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      nextFieldOffset = dlls1.getPrevOffset();
    } else {
      isRecoverableFailure = true;
      return;
    }
    // TODO Is the used size the correct one?
    Optional<SMGHasValueEdge> edgeToNextSmgValue =
        inputSMG1.getHasValueEdgeByPredicate(
            dlls1,
            edge ->
                nextFieldOffset.equals(edge.getOffset())
                    && dlls1.getSize().equals(edge.getSizeInBits()));

    SMGValue nextValue = edgeToNextSmgValue.orElseThrow().hasValue();

    SMGObject mappedObject = mapping1.getMappedObject(dlls1);
    if (mappedObject != null) {
      updateExistingJoin(
          mappedObject,
          pValue1,
          pValue2,
          nextValue,
          nextFieldOffset,
          pNestingLevelDiff,
          pToEdge1.targetSpecifier());
      return;
    }
    // step 5
    if (checkIfNotMatchingMappingsExists(nextValue, pValue2)) {
      isRecoverableFailure = true;
      return;
    }
    // step 6
    status =
        status.updateWith(
            dlls1.getMinLength() == 0 ? SMGJoinStatus.LEFT_ENTAIL : SMGJoinStatus.EQUAL);
    // step 7 & 8 - copy and labeling
    SMGDoublyLinkedListSegment freshCopyDLLS1 =
        new SMGDoublyLinkedListSegment(
            dlls1.getNestingLevel(),
            dlls1.getSize(),
            dlls1.getOffset(),
            dlls1.getHeadOffset(),
            dlls1.getNextOffset(),
            dlls1.getPrevOffset(),
            0);
    mapping1.addMapping(dlls1, freshCopyDLLS1);
    destSMG = destSMG.copyAndAddObject(freshCopyDLLS1);
    // step 7 map unmapped nodes and exclude already mapped
    recursiveCopyMapAndAddObject(dlls1, pNestingLevelDiff);

    // step 9
    Optional<SMGValue> resultOptional =
        destSMG.findAddressForEdge(
            freshCopyDLLS1, pToEdge1.getOffset(), pToEdge1.targetSpecifier());
    if (resultOptional.isEmpty()) {
      value = SMGValue.of(pValue1.getNestingLevel() + pNestingLevelDiff);
      mapping1.addMapping(pValue1, value);
      destSMG =
          destSMG
              .copyAndAddValue(value)
              .copyAndAddPTEdge(
                  new SMGPointsToEdge(
                      freshCopyDLLS1, pToEdge1.getOffset(), pToEdge1.targetSpecifier()),
                  value);
    } else {
      value = resultOptional.orElseThrow();
    }

    // step 10
    SMGJoinValues joinValues =
        new SMGJoinValues(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            nextValue,
            pValue2,
            pNestingLevelDiff);
    if (!joinValues.isDefined) {
      isDefined = false;
      return;
    }
    // step 11 & 12
    SMGHasValueEdge resultHasValueEdge =
        new SMGHasValueEdge(value, nextFieldOffset, dlls1.getSize());
    destSMG = destSMG.copyAndAddHVEdge(resultHasValueEdge, freshCopyDLLS1);
  }

  /**
   * Helper function for step 7 to recursive map list nodes.
   *
   * @param objectToBeCopied - the head object to be copied
   * @param pNestingLevelDiff - the nesting level difference
   */
  private void recursiveCopyMapAndAddObject(SMGObject objectToBeCopied, int pNestingLevelDiff) {
    inputSMG1
        .getEdges(objectToBeCopied)
        .forEach(
            edge -> {
              // TODO is this check needed??
              // if (!isBorderEdge(objectToBeCopied, edge)) {
              SMGValue subSmgValue = edge.hasValue();
              SMGObject mappedSubSmgObject = mapping1.getMappedObject(objectToBeCopied);
              SMGValue mappedSubSmgValue = subSmgValue;

              if (!subSmgValue.isZero() && inputSMG1.isPointer(subSmgValue)) {
                // Safe because asserted by if condition (isPointer())
                SMGPointsToEdge subSMGPointerEdge = inputSMG1.getPTEdge(subSmgValue).orElseThrow();
                SMGObject subSmgObject = subSMGPointerEdge.pointsTo();
                // map object
                if (!mapping1.hasMapping(subSmgObject)) {
                  mappedSubSmgObject =
                      subSmgObject.copyWithNewLevel(
                          subSmgObject.getNestingLevel() + pNestingLevelDiff);
                  destSMG = destSMG.copyAndAddObject(mappedSubSmgObject);
                  mapping1.addMapping(subSmgObject, mappedSubSmgObject);
                  // recursive copy all objects
                  recursiveCopyMapAndAddObject(subSmgObject, pNestingLevelDiff);
                }
                // map values
                if (!mapping1.hasMapping(subSmgValue)) {
                  mappedSubSmgValue = mapping1.getMappedValue(subSmgValue);
                } else {
                  mappedSubSmgValue = SMGValue.of(subSmgValue.getNestingLevel());
                  destSMG = destSMG.copyAndAddValue(mappedSubSmgValue);
                  mapping1.addMapping(subSmgValue, mappedSubSmgValue);
                }
                // copy and add edges
                SMGPointsToEdge newEdge =
                    new SMGPointsToEdge(
                        mappedSubSmgObject,
                        subSMGPointerEdge.getOffset(),
                        subSMGPointerEdge.targetSpecifier());
                destSMG = destSMG.copyAndAddPTEdge(newEdge, mappedSubSmgValue);
                // }
              }
              SMGHasValueEdge hValueEdge =
                  new SMGHasValueEdge(mappedSubSmgValue, edge.getOffset(), edge.getSizeInBits());
              if (!destSMG.hasOverlappingEdge(hValueEdge, mappedSubSmgObject)) {
                if (!destSMG.getValues().contains(mappedSubSmgValue)) {
                  // TODO this is the case if subSmgValue == mappedSubSmgValue, does this make
                  // sense?
                  destSMG = destSMG.copyAndAddValue(mappedSubSmgValue);
                }
                if (!mapping1.hasMapping(subSmgValue)) {
                  // TODO this is the case if subSmgValue == mappedSubSmgValue, does this make
                  // sense?
                  mapping1.addMapping(subSmgValue, mappedSubSmgValue);
                }

                destSMG = destSMG.copyAndAddHVEdge(hValueEdge, mappedSubSmgObject);
              }
            });
  }

  /**
   * Check whether there are already mappings for two value nodes and if the mappings are equal.
   *
   * @param value1 - the first value
   * @param value2 - the second value
   * @return true only if there is a mapping for both values and if those are equal.
   */
  private boolean checkIfNotMatchingMappingsExists(SMGValue value1, SMGValue value2) {
    return mapping1.hasMapping(value1)
        && mapping2.hasMapping(value2)
        && mapping1.getMappedValue(value1).equals(mapping2.getMappedValue(value2));
  }

  // step 4
  /**
   * Helper function for step 4 to update an existing join on same values.
   *
   * @param pMappedObject of the join
   * @param pValue1 to be joined
   * @param pValue2 to be joined with
   * @param nextValue result
   * @param offset of the join
   * @param pNestingLevelDiff of the both values
   * @param targetSpecifier of the region
   */
  private void updateExistingJoin(
      SMGObject pMappedObject,
      SMGValue pValue1,
      SMGValue pValue2,
      SMGValue nextValue,
      BigInteger offset,
      int pNestingLevelDiff,
      SMGTargetSpecifier targetSpecifier) {
    // step 4 - 2
    if (mapping2.mappingExists(pMappedObject)) {
      isRecoverableFailure = true;
      return;
    }
    // step 4 - 3
    if (!mapping1.hasMapping(pValue1)) {
      value = SMGValue.of(pValue1.getNestingLevel());
      destSMG = destSMG.copyAndAddValue(value);
      SMGPointsToEdge edge = new SMGPointsToEdge(pMappedObject, offset, targetSpecifier);
      destSMG = destSMG.copyAndAddPTEdge(edge, value);
      mapping1.addMapping(pValue1, value);
    } else {
      value = mapping1.getMappedValue(pValue1);
      return;
    }
    // step 4-4
    SMGJoinValues joinValues =
        new SMGJoinValues(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            nextValue,
            pValue2,
            pNestingLevelDiff);
    isDefined = joinValues.isDefined();
  }

  /**
   * Sanity check whether pointer actually points to DLLs.
   *
   * @param pValue - the pointer to be checked.
   */
  private void checkPointsToDLLs(SMGValue pValue) {
    Optional<SMGPointsToEdge> edgeOptionalV = inputSMG1.getPTEdge(pValue);
    checkArgument(edgeOptionalV.isPresent());

    SMGPointsToEdge pToEdge = edgeOptionalV.orElseThrow();
    SMGObject object = pToEdge.pointsTo();
    checkArgument(isDLLS(object), "Value 1 does not point to dlls.");
  }
}
