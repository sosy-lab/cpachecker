// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.StructHandlingStrategy;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;

/**
 * Utility class for handling assignments involving unions and structs according to the selected
 * StructHandlingStrategy.
 */
public class StructUnionHandler {

  public static boolean isStruct(CType cType) {
    return cType.getCanonicalType() instanceof CComplexType complexType
        && complexType.getKind() == ComplexTypeKind.STRUCT;
  }

  public static boolean isUnion(CType cType) {
    return cType.getCanonicalType() instanceof CComplexType complexType
        && complexType.getKind() == ComplexTypeKind.UNION;
  }

  /**
   * Handle an assignment to a field whose owner type is either a struct or a union.
   *
   * <p>Dispatches to the appropriate strategy-specific implementation based on {@code baseType}. If
   * {@code baseType} is neither struct nor union, this method is a no-op and returns {@code
   * pState}.
   */
  public static PointerAnalysisState handleAssignmentForStructOrUnionType(
      PointerAnalysisState pState,
      CType baseType,
      LocationSet lhsLocations,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge,
      StructHandlingStrategy strategy,
      LogManager logger) {
    checkArgument(
        isStruct(baseType) || isUnion(baseType),
        "Expected struct or union baseType, got: %s",
        baseType);

    if (isUnion(baseType)) {
      return handleAssignmentForUnionType(
          pState, lhsLocations, rhsTargets, pCfaEdge, strategy, logger);
    }
    if (isStruct(baseType)) {
      return handleAssignmentForStructType(
          pState, lhsLocations, rhsTargets, pCfaEdge, strategy, logger);
    }
    return pState;
  }

  private static PointerAnalysisState handleAssignmentForUnionType(
      PointerAnalysisState pState,
      LocationSet lhsLocations,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge,
      StructHandlingStrategy strategy,
      LogManager logger) {

    if (lhsLocations.isBot()) {
      return PointerAnalysisState.BOTTOM_STATE;
    }

    if (lhsLocations instanceof ExplicitLocationSet explicitLhsLocations) {
      if (explicitLhsLocations.getSize() == 1) {
        Optional<PointerAnalysisState> specialCase =
            PointerUtils.handleSpecialCasesForExplicitLocation(
                pState, explicitLhsLocations, rhsTargets, pCfaEdge, logger);

        if (specialCase.isPresent()) {
          return specialCase.orElseThrow();
        }

        PointerLocation lhsLocation =
            explicitLhsLocations.sortedPointerLocations().iterator().next();

        if (strategy == StructHandlingStrategy.JUST_STRUCT) {
          LocationSet existingSet = pState.getPointsToSet(lhsLocation);
          if (existingSet.isTop()) {
            return new PointerAnalysisState(
                pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
          }
          LocationSet mergedSet = existingSet.withPointerTargets(rhsTargets);
          return new PointerAnalysisState(
              pState.getPointsToMap().putAndCopy(lhsLocation, mergedSet));
        } else {
          return new PointerAnalysisState(
              pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
        }
      } else {
        return addElementsToAmbiguousLocations(pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }

  private static PointerAnalysisState handleAssignmentForStructType(
      PointerAnalysisState pState,
      LocationSet lhsLocations,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge,
      StructHandlingStrategy strategy,
      LogManager logger) {

    if (lhsLocations.isBot()) {
      return PointerAnalysisState.BOTTOM_STATE;
    }

    if (lhsLocations instanceof ExplicitLocationSet explicitLhsLocations) {
      if (explicitLhsLocations.getSize() == 1) {
        Optional<PointerAnalysisState> specialCase =
            PointerUtils.handleSpecialCasesForExplicitLocation(
                pState, explicitLhsLocations, rhsTargets, pCfaEdge, logger);

        if (specialCase.isPresent()) {
          return specialCase.orElseThrow();
        }

        PointerLocation lhsLocation =
            explicitLhsLocations.sortedPointerLocations().iterator().next();

        if (strategy == StructHandlingStrategy.ALL_FIELDS) {
          return new PointerAnalysisState(
              pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
        } else {
          LocationSet existingSet = pState.getPointsToSet(lhsLocation);
          if (existingSet.isTop()) {
            return new PointerAnalysisState(
                pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets));
          }
          LocationSet mergedSet = existingSet.withPointerTargets(rhsTargets);
          return new PointerAnalysisState(
              pState.getPointsToMap().putAndCopy(lhsLocation, mergedSet));
        }
      } else {
        return addElementsToAmbiguousLocations(pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }

  public static PointerAnalysisState addElementsToAmbiguousLocations(
      PointerAnalysisState pState, ExplicitLocationSet pLhsLocations, LocationSet pRhsTargets) {
    Set<PointerLocation> locations = pLhsLocations.sortedPointerLocations();

    PointerAnalysisState updatedState = pState;

    for (PointerLocation loc : locations) {
      if (pRhsTargets.isTop()) {
        if (updatedState.getPointsToMap().containsKey(loc)) {
          updatedState = new PointerAnalysisState(updatedState.getPointsToMap().removeAndCopy(loc));
        }
        continue;
      }

      LocationSet existingSet = updatedState.getPointsToSet(loc);
      LocationSet mergedSet = existingSet.withPointerTargets(pRhsTargets);
      updatedState =
          new PointerAnalysisState(updatedState.getPointsToMap().putAndCopy(loc, mergedSet));
    }
    return updatedState;
  }
}
