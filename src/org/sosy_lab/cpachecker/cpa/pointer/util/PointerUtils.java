// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.HeapLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.StructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class PointerUtils {

  private PointerUtils() {}

  public static boolean isFreeFunction(CExpression pExpression) {
    return getFunctionName(pExpression).map("free"::equals).orElse(false);
  }

  public static boolean isMallocFunction(CExpression pExpression) {
    return getFunctionName(pExpression).map("malloc"::equals).orElse(false);
  }

  public static boolean isNondetPointerReturn(CExpression pExpression) {
    return getFunctionName(pExpression).map("__VERIFIER_nondet_pointer"::equals).orElse(false);
  }

  private static Optional<String> getFunctionName(CExpression pExpression) {
    if (pExpression instanceof CIdExpression idExpr) {
      return Optional.of(idExpr.getName());
    }
    return Optional.empty();
  }

  public static boolean isValidFunctionReturn(PointerLocation pTarget, String currentFunctionName) {
    if (pTarget instanceof HeapLocation) {
      return true;
    }
    if (pTarget instanceof DeclaredVariableLocation ptr) {
      return !ptr.isLocalVariable()
          || ptr.memoryLocation().getFunctionName().equals(currentFunctionName);
    }
    if (pTarget instanceof StructLocation structLoc) {
      return !structLoc.isOnFunctionStack()
          || currentFunctionName.equals(structLoc.getFunctionName());
    }
    return true;
  }

  public static PointerAnalysisState handleTopAssignmentCase(
      PointerAnalysisState pState, PointerLocation lhsLocation) {
    if (pState.getPointsToMap().containsKey(lhsLocation)) {
      return new PointerAnalysisState(pState.getPointsToMap().removeAndCopy(lhsLocation));
    }
    return pState;
  }

  public static Optional<PointerAnalysisState> handleSpecialCasesForExplicitLocation(
      PointerAnalysisState pState,
      LocationSet pLocationSet,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge,
      LogManager plogger) {
    if (pLocationSet.containsAllNulls()) {
      plogger.logf(
          Level.WARNING, "PointerAnalysis: Assignment to null at %s", pCfaEdge.getFileLocation());
      return Optional.of(PointerAnalysisState.BOTTOM_STATE);
    }

    if (pLocationSet instanceof ExplicitLocationSet pExplicitLocationSet) {
      PointerLocation lhsLocation = pExplicitLocationSet.sortedPointerLocations().iterator().next();

      if (lhsLocation instanceof InvalidLocation) {
        plogger.logf(
            Level.WARNING,
            "PointerAnalysis: Assignment to invalid location %s at %s",
            lhsLocation,
            pCfaEdge.getFileLocation());
        return Optional.of(PointerAnalysisState.BOTTOM_STATE);
      }

      if (rhsTargets.isTop()) {
        return Optional.of(PointerUtils.handleTopAssignmentCase(pState, lhsLocation));
      }
    }

    return Optional.empty();
  }

  public static boolean isNullPointer(CExpression pExpression) {
    if (pExpression instanceof CCastExpression castExpression) {
      CExpression operand = castExpression.getOperand();
      if (operand instanceof CIntegerLiteralExpression intLiteral
          && intLiteral.getValue().longValue() == 0) {
        return true;
      }
    }
    return pExpression instanceof CIntegerLiteralExpression intLiteral
        && intLiteral.getValue().longValue() == 0;
  }

  public static boolean isNullPointer(CRightHandSide pRhs) {
    return (pRhs instanceof CExpression cExpr) && isNullPointer(cExpr);
  }

  public static LocationSet toLocationSet(MemoryLocation pLocation) {
    return toLocationSet(Collections.singleton(pLocation));
  }

  public static LocationSet toLocationSet(Set<MemoryLocation> pLocations) {
    if (pLocations == null) {
      return LocationSetFactory.withTop();
    }
    if (pLocations.isEmpty()) {
      return LocationSetFactory.withBot();
    }
    Set<PointerLocation> locations = new HashSet<>();
    for (MemoryLocation loc : pLocations) {
      locations.add(new DeclaredVariableLocation(loc));
    }
    return LocationSetFactory.withPointerTargets(locations);
  }

  public static boolean hasCommonLocation(ExplicitLocationSet pSet1, LocationSet pSet2) {
    if (pSet1.containsAnyNull() && pSet2.containsAnyNull()) {
      return true;
    }
    for (PointerLocation loc : pSet1.sortedPointerLocations()) {
      if (pSet2.contains(loc)) {
        return true;
      }
    }
    return false;
  }

  public static Optional<MemoryLocation> getFunctionReturnVariable(
      FunctionEntryNode pFunctionEntryNode) {
    Optional<? extends AVariableDeclaration> returnVariable =
        pFunctionEntryNode.getReturnVariable();
    return returnVariable.map(MemoryLocation::forDeclaration);
  }
}
