// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class OverapproximatingMemoryLocation {

  // The potential memory locations if isPrecise is true, otherwise the variables that may contain
  // the location (empty if unknown)
  private final ImmutableSet<MemoryLocation> memoryLocations;
  private final CType type;
  private final boolean isAmbiguous;
  private final boolean isPrecise;
  // Whether a pointer may point to this location, i.e., its address is taken or it is an array
  private final boolean mayBePointedTo;
  // The accessed lvalue
  private final CExpression expression;
  // The locations read to compute the address of an imprecise location
  private final ImmutableSet<OverapproximatingMemoryLocation> addressReads;

  /**
   * Create an {@link OverapproximatingMemoryLocation} for the given lvalue that may represent any
   * memory location of its type within the given variables, or anywhere if they are empty.
   */
  public OverapproximatingMemoryLocation(
      CExpression pExpression,
      Set<OverapproximatingMemoryLocation> pAddressReads,
      Set<MemoryLocation> pContainingVariables) {
    this(
        pContainingVariables,
        pExpression.getExpressionType(),
        true,
        false,
        true,
        pExpression,
        pAddressReads);
  }

  public OverapproximatingMemoryLocation(
      Set<MemoryLocation> pMemoryLocations,
      CType pType,
      boolean pIsAmbiguous,
      boolean pIsPrecise,
      boolean pMayBePointedTo,
      CExpression pExpression) {
    this(
        pMemoryLocations,
        pType,
        pIsAmbiguous,
        pIsPrecise,
        pMayBePointedTo,
        pExpression,
        ImmutableSet.of());
  }

  private OverapproximatingMemoryLocation(
      Set<MemoryLocation> pMemoryLocations,
      CType pType,
      boolean pIsAmbiguous,
      boolean pIsPrecise,
      boolean pMayBePointedTo,
      CExpression pExpression,
      Set<OverapproximatingMemoryLocation> pAddressReads) {
    memoryLocations = ImmutableSet.copyOf(pMemoryLocations);
    type = pType;
    isAmbiguous = pIsAmbiguous;
    isPrecise = pIsPrecise;
    mayBePointedTo = pMayBePointedTo;
    expression = pExpression;
    addressReads = ImmutableSet.copyOf(pAddressReads);
  }

  public Set<MemoryLocation> getMemoryLocations() {
    return memoryLocations;
  }

  public CType getType() {
    return type;
  }

  public boolean isAmbiguous() {
    return isAmbiguous;
  }

  public boolean isPrecise() {
    return isPrecise;
  }

  public boolean mayBePointedTo() {
    return mayBePointedTo;
  }

  public CExpression getExpression() {
    return expression;
  }

  public Set<OverapproximatingMemoryLocation> getAddressReads() {
    return addressReads;
  }

  /**
   * Checks whether this location may overlap the given one, regardless of their types. Only a
   * location that may be pointed to can overlap an imprecise one.
   */
  public boolean mayOverlap(OverapproximatingMemoryLocation pOther) {
    if (!(isPrecise && pOther.isPrecise) && !(mayBePointedTo && pOther.mayBePointedTo)) {
      return false;
    }
    if (memoryLocations.isEmpty() || pOther.memoryLocations.isEmpty()) {
      return true;
    }
    for (MemoryLocation location : memoryLocations) {
      for (MemoryLocation otherLocation : pOther.memoryLocations) {
        if (contains(location, otherLocation) || contains(otherLocation, location)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean contains(MemoryLocation pOuter, MemoryLocation pInner) {
    return pOuter.equals(pInner)
        || pInner.getExtendedQualifiedName().startsWith(pOuter.getExtendedQualifiedName() + ".");
  }

  @Override
  public String toString() {
    if (isPrecise) {
      return memoryLocations.toString();
    }
    return "OverapproximatingMemoryLocation{" + type + "}";
  }
}
