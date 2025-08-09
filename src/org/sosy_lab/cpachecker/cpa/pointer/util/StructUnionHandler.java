// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisTransferRelation.PointerTransferOptions.StructHandlingStrategy;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.StructLocation;

/**
 * Utility class for handling assignments involving unions and structs according to the selected
 * StructHandlingStrategy.
 */
public class StructUnionHandler {

  public static boolean isStruct(Type pType) {
    return pType instanceof CType cType
        && cType.getCanonicalType() instanceof CComplexType complexType
        && complexType.getKind() == ComplexTypeKind.STRUCT;
  }

  public static boolean isUnion(Type pType) {
    return pType instanceof CType cType
        && cType.getCanonicalType() instanceof CComplexType complexType
        && complexType.getKind() == ComplexTypeKind.UNION;
  }

  public static PointerAnalysisState handleUnionAssignment(
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

        PointerLocation lhsLocation = explicitLhsLocations.sortedPointerLocations().iterator().next();

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
        return addElementsToAmbiguousLocations(
            pState, explicitLhsLocations, rhsTargets, strategy, true);
      }
    }
    return pState;
  }

  public static PointerAnalysisState handleStructAssignment(
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

        PointerLocation lhsLocation = explicitLhsLocations.sortedPointerLocations().iterator().next();

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
        return addElementsToAmbiguousLocations(
            pState, explicitLhsLocations, rhsTargets, strategy, false);
      }
    }
    return pState;
  }

  private static PointerAnalysisState addElementsToAmbiguousLocations(
      PointerAnalysisState pState,
      ExplicitLocationSet pLhsLocations,
      LocationSet pRhsTargets,
      StructHandlingStrategy strategy,
      boolean isUnion) {

    Set<PointerLocation> locations = pLhsLocations.sortedPointerLocations();
    PointerAnalysisState updatedState = pState;

    for (PointerLocation loc : locations) {
      if (pRhsTargets.isTop()) {
        if (updatedState.getPointsToMap().containsKey(loc)) {
          updatedState = new PointerAnalysisState(pState.getPointsToMap().removeAndCopy(loc));
        }
        continue;
      }
      boolean mergeTargetsOfUnions = isUnion && strategy == StructHandlingStrategy.JUST_STRUCT;
      boolean mergeTargetsOfStructs = !isUnion && strategy != StructHandlingStrategy.ALL_FIELDS;
      boolean shouldMerge = mergeTargetsOfUnions || mergeTargetsOfStructs;

      if (shouldMerge) {
        LocationSet existingSet = updatedState.getPointsToSet(loc);
        LocationSet mergedSet = existingSet.withPointerTargets(pRhsTargets);
        updatedState =
            new PointerAnalysisState(updatedState.getPointsToMap().putAndCopy(loc, mergedSet));
      } else {
        updatedState =
            new PointerAnalysisState(updatedState.getPointsToMap().putAndCopy(loc, pRhsTargets));
      }
    }
    return updatedState;
  }

  public static LocationSet getUnionLocation(
      StructHandlingStrategy strategy, String structType, String instanceName, CFAEdge pCfaEdge) {
    return switch (strategy) {
      case ALL_FIELDS, STRUCT_INSTANCE ->
          LocationSetFactory.withPointerLocation(
              StructLocation.forStructInstance(
                  pCfaEdge.getPredecessor().getFunctionName(), structType, instanceName));
      case JUST_STRUCT ->
          LocationSetFactory.withPointerLocation(
              StructLocation.forStruct(pCfaEdge.getPredecessor().getFunctionName(), structType));
    };
  }

  public static LocationSet getStructLocation(
      StructHandlingStrategy strategy,
      String structType,
      String instanceName,
      String fieldName,
      CFAEdge pCfaEdge) {
    return switch (strategy) {
      case STRUCT_INSTANCE ->
          LocationSetFactory.withPointerLocation(
              StructLocation.forStructInstance(
                  pCfaEdge.getPredecessor().getFunctionName(), structType, instanceName));
      case ALL_FIELDS ->
          LocationSetFactory.withPointerLocation(
              StructLocation.forField(
                  pCfaEdge.getPredecessor().getFunctionName(),
                  structType,
                  instanceName,
                  fieldName));
      case JUST_STRUCT ->
          LocationSetFactory.withPointerLocation(
              StructLocation.forStruct(pCfaEdge.getPredecessor().getFunctionName(), structType));
    };
  }
}

