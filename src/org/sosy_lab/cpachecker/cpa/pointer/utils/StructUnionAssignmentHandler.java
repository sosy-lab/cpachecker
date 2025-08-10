// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.utils;

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
 * Implements the logic for pointer assignments involving C structs and unions.
 *
 * <p>The behavior is highly dependent on the chosen {@link StructHandlingStrategy}, which allows
 * for a trade-off between analysis precision and performance. This handler dispatches to the
 * appropriate logic based on the aggregate type and the configured strategy. It also correctly
 * distinguishes between **strong updates** (where the LHS location is known precisely) and **weak
 * updates** (where the LHS is ambiguous and could be one of several locations).
 */
public class StructUnionAssignmentHandler {

  public static boolean isStruct(CType cType) {
    return cType.getCanonicalType() instanceof CComplexType complexType
        && complexType.getKind() == ComplexTypeKind.STRUCT;
  }

  public static boolean isUnion(CType cType) {
    return cType.getCanonicalType() instanceof CComplexType complexType
        && complexType.getKind() == ComplexTypeKind.UNION;
  }

  /**
   * Handles an assignment to a field of a struct or a union.
   *
   * <p>This is the main entry point for this handler. It acts as a dispatcher, delegating to more
   * specific methods based on whether the {@code baseType} is a struct or a union, and applies the
   * logic defined by the given {@code strategy}.
   *
   * @param pState The current pointer analysis state.
   * @param baseType The type of the aggregate owner (must be a struct or union).
   * @param lhsLocations The set of possible locations for the LHS field.
   * @param rhsTargets The set of locations the RHS points to.
   * @param pCfaEdge The current CFA edge, used for logging.
   * @param strategy The configured {@link StructHandlingStrategy} to apply.
   * @param logger The logger for reporting warnings.
   * @return The updated {@link PointerAnalysisState}.
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
        PointerLocation lhsLocation =
            explicitLhsLocations.sortedPointerLocations().iterator().next();

        Optional<PointerAnalysisState> edgeCaseStateOptional =
            AssignmentHandler.handleAssignmentEdgeCases(
                pState, lhsLocation, rhsTargets, pCfaEdge, logger);

        if (edgeCaseStateOptional.isPresent()) {
          return edgeCaseStateOptional.get();
        }

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
        return performWeakUpdate(pState, explicitLhsLocations, rhsTargets);
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
        PointerLocation lhsLocation =
            explicitLhsLocations.sortedPointerLocations().iterator().next();

        Optional<PointerAnalysisState> edgeCaseStateOptional =
            AssignmentHandler.handleAssignmentEdgeCases(
                pState, lhsLocation, rhsTargets, pCfaEdge, logger);

        if (edgeCaseStateOptional.isPresent()) {
          return edgeCaseStateOptional.get();
        }

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
        return performWeakUpdate(pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }

  /**
   * Performs a **weak update** on the points-to map.
   *
   * <p>A weak update is necessary when the LHS of an assignment is ambiguous and could refer to one
   * of several possible memory locations. Instead of replacing the points-to set of a single
   * location (a strong update), we must merge the new information into the points-to sets of all
   * potential LHS locations.
   *
   * @param pState The current analysis state.
   * @param pLhsLocations The explicit set of possible LHS locations to update.
   * @param pRhsTargets The new targets from the RHS to be merged into the points-to sets.
   * @return The new analysis state after the weak update.
   */
  public static PointerAnalysisState performWeakUpdate(
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
