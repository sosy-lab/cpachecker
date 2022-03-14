// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.ValueAndObjectSet;

public class SMGProveNequality {

  private final SMG smg;

  public SMGProveNequality(SMG pSMG) {
    smg = pSMG;
  }

  /**
   * Implementation of Algorithm 13 Appendix E. Tries to prove the not equality of two given
   * addresses. Returns true if the prove of not equality succeeded, returns false if both are
   * potentially equal.
   *
   * @param value1 the first address
   * @param value2 the second address
   * @return true if the prove of not equality succeeded, false if both are potentially equal.
   */
  public boolean proveInequality(SMGValue value1, SMGValue value2) {
    checkArgument(
        value1.getNestingLevel() == 0 && value2.getNestingLevel() == 0,
        "%s or %s is not on level 0",
        value1,
        value2);
    if (value1.equals(value2)) {
      return false;
    }

    ValueAndObjectSet targetValueAndReachedSet1 = lookThrough(value1);
    ValueAndObjectSet targetValueAndReachedSet2 = lookThrough(value2);
    // check if values are equal
    if (targetValueAndReachedSet1.getValue().equals(targetValueAndReachedSet2.getValue())) {
      return false;
    }
    // check if reached sets share regions
    if (targetValueAndReachedSet1.getObjectSet().stream()
        .anyMatch(targetValueAndReachedSet2.getObjectSet()::contains)) {
      return false;
    }
    // simplified handling of data values
    if (!smg.isPointer(targetValueAndReachedSet1.getValue())
        || !smg.isPointer(targetValueAndReachedSet2.getValue())) {
      return false;
    }
    // Safe Optional.get()_ implicit exists check performed with isPointer
    SMGPointsToEdge targetEdge1 = smg.getPTEdge(targetValueAndReachedSet1.getValue()).orElseThrow();
    SMGPointsToEdge targetEdge2 = smg.getPTEdge(targetValueAndReachedSet2.getValue()).orElseThrow();
    if (targetEdge1.pointsTo().equals(targetEdge2.pointsTo())) {
      return checkEdgeLabelsForEqualTargets(targetEdge1, targetEdge2);
    }
    // OutOfBounds check
    if (checkIfEdgePointsOutOfBounds(targetEdge1) || checkIfEdgePointsOutOfBounds(targetEdge2)) {
      return false;
    }
    // 0 and a valid address of an object
    if (targetValueAndReachedSet1.getValue().isZero()
        || targetValueAndReachedSet2.getValue().isZero()) {
      return true;
    }
    // addresses of allocated objects
    return smg.isValid(targetEdge1.pointsTo()) && smg.isValid(targetEdge2.pointsTo());
  }

  private boolean checkIfEdgePointsOutOfBounds(SMGPointsToEdge pToEdge) {
    return pToEdge.getOffset().compareTo(pToEdge.pointsTo().getSize()) > 0
        || pToEdge.getOffset().signum() < 0;
  }

  private boolean checkEdgeLabelsForEqualTargets(
      SMGPointsToEdge pTagetEdge1, SMGPointsToEdge pTagetEdge2) {
    // same object, different offsets
    if (pTagetEdge1.targetSpecifier().equals(pTagetEdge2.targetSpecifier())) {
      return true;
    }
    if ((pTagetEdge1.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)
            || pTagetEdge1.targetSpecifier().equals(SMGTargetSpecifier.IS_LAST_POINTER))
        && (pTagetEdge2.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)
            || pTagetEdge2.targetSpecifier().equals(SMGTargetSpecifier.IS_LAST_POINTER))) {
      return ((SMGDoublyLinkedListSegment) pTagetEdge1.pointsTo()).getMinLength() >= 2;
    }
    return false;
  }

  /**
   * Implementation of Appendix E Algorithm 12. Traverse all 0+DLLs and collect all visited
   * SMGObjects as well as the final reached value.
   *
   * @param value - the first address value representation.
   * @return the finally reached value and the set of all visited objects.
   */
  public ValueAndObjectSet lookThrough(SMGValue value) {
    Set<SMGObject> reachedSet = new HashSet<>();
    SMGValue retValue = value;
    Optional<SMGPointsToEdge> ptoOptional = smg.getPTEdge(value);
    while (ptoOptional.isPresent() && !ptoOptional.orElseThrow().pointsTo().isZero()) {
      SMGPointsToEdge pointerEdge = ptoOptional.orElseThrow();
      SMGObject nextObject = pointerEdge.pointsTo();
      checkArgument(nextObject instanceof SMGDoublyLinkedListSegment);

      SMGDoublyLinkedListSegment dlls = (SMGDoublyLinkedListSegment) nextObject;
      if (dlls.getMinLength() != 0) {
        // not a 0+DLLS
        break;
      }

      reachedSet.add(nextObject);
      if (pointerEdge.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
        retValue = findHVETargetValue(dlls, dlls.getNextOffset(), smg.getSizeOfPointer());

      } else {
        checkArgument(
            pointerEdge.targetSpecifier().equals(SMGTargetSpecifier.IS_LAST_POINTER),
            "Inconsisntent SMG found: DLLS pointer with SMGTargetSpecifier: %s",
            pointerEdge.targetSpecifier().name());
        retValue = findHVETargetValue(dlls, dlls.getPrevOffset(), smg.getSizeOfPointer());
      }
      ptoOptional = smg.getPTEdge(retValue);
    }
    return new ValueAndObjectSet(reachedSet, retValue);
  }

  /**
   * Utility function to find the edge for a given dlls at a given offset with a given size.
   *
   * @param dlls the DLLS
   * @param pOffset the offset
   * @param pSize the size
   * @return the value address (pointer) at a given offset of a dlls.
   */
  private SMGValue findHVETargetValue(
      SMGDoublyLinkedListSegment dlls, BigInteger pOffset, BigInteger pSize) {
    Optional<SMGHasValueEdge> hveOptional =
        smg.getHasValueEdgeByPredicate(
            dlls, edge -> edge.getOffset().equals(pOffset) && edge.getSizeInBits().equals(pSize));
    checkArgument(
        hveOptional.isPresent(),
        "No pointer for DLLS %s with offset %s and Size %s found.",
        dlls,
        pOffset,
        pSize);
    return hveOptional.orElseThrow().hasValue();
  }
}
