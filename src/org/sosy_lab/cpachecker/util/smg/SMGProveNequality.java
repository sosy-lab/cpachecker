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
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.ValueAndObjectSet;

public class SMGProveNequality {

  private final SMGState state;
  private final SMGOptions options;

  public SMGProveNequality(SMGState pState, SMGOptions pOptions) {
    state = pState;
    options = pOptions;
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
  public boolean proveInequality(SMGValue value1, SMGValue value2) throws SMGSolverException {
    SMG smg = state.getMemoryModel().getSmg();
    // The nesting level should always be 0, as we only compare materialized SMGs
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
    if (checkPointsToEdgeOutOfBounds(targetEdge1) || checkPointsToEdgeOutOfBounds(targetEdge2)) {
      if (targetEdge1.pointsTo().isZero() || targetEdge2.pointsTo().isZero()) {
        // Out-of-bounds and a null pointer, may be equal
        // TODO: add case where we are only 1 object beyond the size, as that's defined and can not
        // be equal null!
        return false;
      }
      if (!options.isOverapproximatePointerArithmeticsOutOfBoundsEquality()) {
        // TODO: make the result of this method a status style system like in merge! We can't
        // discern between "can be both" and not equal currently.
        return !checkPointsToEdgesOutOfBoundsEquality(targetEdge1, targetEdge2);
      }
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

  /**
   * Checks whether the {@link SMGPointsToEdge}s, with at least one being out-of-bounds, for their
   * (possibly distinct targeting objects) equality. This checks whether there is enough space
   * after/before the pointer and its memory for the other memory, for example:
   *
   * <p>int * ptr1 = malloc(2*sizeof(int));
   *
   * <p>int * ptr2 = malloc(2*sizeof(int));
   *
   * <p>assert((ptr1 + 2) == (ptr2 + 1)); // Always false
   *
   * <p>assert((ptr1 + 2) == ptr2); // Can be true or false
   *
   * <p>Since we may cast the addresses to numbers and then do the checks above, we can even do
   * things like:
   *
   * <p>assert((((unsigned long) ptr1) - 1) == (unsigned long) ptr2); // Always false, as the memory
   * of ptr2 is too large!
   *
   * <p>assert((((unsigned long) ptr1) - 2) == (unsigned long) ptr2); // Can be true or false
   *
   * @return {@code true} if the 2 {@link SMGPointsToEdge}s CAN be equal (but may also be inequal),
   *     {@code false} if they are guaranteed to be not equal.
   */
  protected boolean checkPointsToEdgesOutOfBoundsEquality(
      SMGPointsToEdge pte1, SMGPointsToEdge pte2) {
    SMGObject targetObj1 = pte1.pointsTo();
    SMGObject targetObj2 = pte2.pointsTo();
    Value targetObjSize1 = pte1.pointsTo().getSize();
    Value targetObjSize2 = pte2.pointsTo().getSize();
    Value pointerOffset1 = pte1.getOffset();
    Value pointerOffset2 = pte2.getOffset();

    checkArgument(targetObj1 != targetObj2); // Should be handled already
    checkArgument(!targetObj1.isZero() && !targetObj2.isZero());
    if (!targetObjSize1.isUnknown()
        && !targetObjSize2.isUnknown()
        && !pointerOffset1.isUnknown()
        && !pointerOffset2.isUnknown()) {
      if (targetObjSize1 instanceof NumericValue numObjSize1
          && pointerOffset1 instanceof NumericValue numPointerOffset1
          && pointerOffset2 instanceof NumericValue numPointerOffset2
          && targetObjSize2 instanceof NumericValue numObjSize2) {

        BigInteger bigIntObjSize1 = numObjSize1.bigIntegerValue();
        BigInteger bigIntObjSize2 = numObjSize2.bigIntegerValue();
        BigInteger bigIntPointerOffset1 = numPointerOffset1.bigIntegerValue();
        BigInteger bigIntPointerOffset2 = numPointerOffset2.bigIntegerValue();
        BigInteger offsetMinusSize1 = bigIntPointerOffset1.subtract(bigIntObjSize1);
        BigInteger offsetMinusSize2 = bigIntPointerOffset2.subtract(bigIntObjSize2);

        if (offsetMinusSize1.compareTo(bigIntPointerOffset2) >= 0
            || offsetMinusSize2.compareTo(bigIntPointerOffset1) >= 0) {
          // TODO: use enum instead of bool
          return true; // Can be equal or not equal
        }
        return false; // Guaranteed to be not-equal!

      } else if (options.trackPredicates()) {
        // Use SMT solver
        // TODO: memory layout check (as with the numerics above)
        throw new UnsupportedOperationException(
            "Precise solver based pointer equality is not yet implemented");
      }
    }

    // Unknown -> Overapproximate, may be equal or inequal
    return true;
  }

  protected boolean checkPointsToEdgeOutOfBounds(SMGPointsToEdge pte) throws SMGSolverException {
    SMGObject targetObj = pte.pointsTo();

    if (targetObj.isZero()) {
      return false; // The null pointer is not out of bounds
    }

    // Unknown -> Overapproximate
    if (!pte.pointsTo().getSize().isUnknown() && !pte.getOffset().isUnknown()) {
      if (targetObj.getSize() instanceof NumericValue targetObjSize
          && pte.getOffset() instanceof NumericValue pToEdgeOffset) {

        // Just "out of bounds" -> overapproximate
        return pToEdgeOffset.bigIntegerValue().compareTo(targetObjSize.bigIntegerValue()) >= 0
            || pToEdgeOffset.bigIntegerValue().signum() < 0;
      } else if (options.trackPredicates()) {
        // At least one symbolic, use SMT solver if possible
        return state
            .checkBoundariesOfMemoryAccessWithSolver(
                targetObj, pte.getOffset(), new NumericValue(BigInteger.ZERO), null)
            .isSAT();
      }
    }
    return true;
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
    SMG smg = state.getMemoryModel().getSmg();
    Set<SMGObject> reachedSet = new HashSet<>();
    SMGValue retValue = value;
    Optional<SMGPointsToEdge> ptoOptional = smg.getPTEdge(value);
    while (ptoOptional.isPresent() && !ptoOptional.orElseThrow().pointsTo().isZero()) {
      SMGPointsToEdge pointerEdge = ptoOptional.orElseThrow();
      SMGObject nextObject = pointerEdge.pointsTo();
      if (pointerEdge.targetSpecifier() == SMGTargetSpecifier.IS_REGION) {
        break;
      }
      checkArgument(nextObject instanceof SMGSinglyLinkedListSegment);

      SMGSinglyLinkedListSegment lls = (SMGSinglyLinkedListSegment) nextObject;
      if (lls.getMinLength() != 0) {
        // not a 0+DLLS
        break;
      }

      reachedSet.add(nextObject);
      if (pointerEdge.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
        retValue = findHVETargetValue(lls, lls.getNextOffset(), smg.getSizeOfPointer());

      } else if (nextObject instanceof SMGDoublyLinkedListSegment dlls) {
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
   * Utility function to find the edge for a given lls at a given offset with a given size.
   *
   * @param dlls the LLS
   * @param pOffset the offset
   * @param pSize the size
   * @return the value address (pointer) at a given offset of a dlls.
   */
  private SMGValue findHVETargetValue(
      SMGSinglyLinkedListSegment dlls, BigInteger pOffset, BigInteger pSize) {
    SMG smg = state.getMemoryModel().getSmg();
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
