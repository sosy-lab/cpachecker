// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import static org.sosy_lab.cpachecker.cpa.pointer.utils.ReferenceLocationsResolver.getReferencedLocations;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidationReason;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.cpa.pointer.utils.AssignmentHandler;
import org.sosy_lab.cpachecker.cpa.pointer.utils.PointerAnalysisChecks;
import org.sosy_lab.cpachecker.cpa.pointer.utils.StructUnionAssignmentHandler;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class FunctionReturnEdgeHandler
    implements TransferRelationEdgeHandler<CFunctionReturnEdge> {
  private final LogManager logger;
  private final PointerTransferOptions pOptions;

  public FunctionReturnEdgeHandler(LogManager pLogger, PointerTransferOptions options) {
    logger = pLogger;
    pOptions = options;
  }

  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, CFunctionReturnEdge pCfaEdge)
      throws CPATransferException {
    CFunctionCall callEdge = pCfaEdge.getFunctionCall();
    if (callEdge instanceof CFunctionCallAssignmentStatement callAssignment) {
      Optional<MemoryLocation> returnVar =
          PointerAnalysisChecks.getFunctionReturnVariable(pCfaEdge.getFunctionEntry());
      if (returnVar.isEmpty()) {
        logger.log(Level.INFO, "Return edge with assignment, but no return variable: " + pCfaEdge);
        return pState;
      }
      DeclaredVariableLocation returnVarPointer =
          new DeclaredVariableLocation(returnVar.orElseThrow());
      CExpression lhs = callAssignment.getLeftHandSide();
      if (!(lhs.getExpressionType() instanceof CPointerType)) {
        return pState;
      }
      LocationSet rhsTargets = pState.getPointsToSet(returnVarPointer);
      if (rhsTargets instanceof ExplicitLocationSet explicitSet) {
        Set<PointerLocation> newTargets = new HashSet<>();

        String callerFunctionName = pCfaEdge.getSummaryEdge().getPredecessor().getFunctionName();
        for (PointerLocation target : explicitSet.sortedPointerLocations()) {
          if (target.isValidFunctionReturn(callerFunctionName)) {
            newTargets.add(target);
          } else {
            newTargets.add(new InvalidLocation(InvalidationReason.LOCAL_SCOPE_EXPIRED));
          }
        }

        rhsTargets = LocationSetFactory.withPointerTargets(newTargets);
      }

      LocationSet lhsLocations = getReferencedLocations(lhs, pState, false, pCfaEdge, pOptions);
      return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
    }
    return pState;
  }

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState,
      LocationSet lhsLocations,
      LocationSet rhsTargets,
      CCfaEdge pCfaEdge) {

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

        return edgeCaseStateOptional.orElseGet(
            () ->
                new PointerAnalysisState(
                    pState.getPointsToMap().putAndCopy(lhsLocation, rhsTargets)));

      } else {
        return StructUnionAssignmentHandler.performWeakUpdate(
            pState, explicitLhsLocations, rhsTargets);
      }
    }
    return pState;
  }
}
