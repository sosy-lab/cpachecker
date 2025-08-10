// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import static org.sosy_lab.cpachecker.cpa.pointer.utils.ReferenceLocationsResolver.getReferencedLocations;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.HeapLocation;
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

public final class StatementEdgeHandler implements TransferRelationEdgeHandler<CStatementEdge> {
  private final LogManager logger;

  private final PointerTransferOptions options;

  private final AtomicInteger allocationCounter;

  public StatementEdgeHandler(
      LogManager pLogger,
      PointerTransferOptions pOptions,
      AtomicInteger allocationCounterAsInteger) {
    logger = pLogger;
    options = pOptions;
    allocationCounter = allocationCounterAsInteger;
  }

  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, CStatementEdge pCfaEdge)
      throws CPATransferException {

    if (pCfaEdge.getStatement() instanceof CFunctionCallStatement callStatement) {
      CFunctionCallExpression callExpr = callStatement.getFunctionCallExpression();

      if (PointerAnalysisChecks.isFreeFunction(callExpr.getFunctionNameExpression())) {
        return handleDeallocation(pState, pCfaEdge, callExpr);
      }
    }

    if (pCfaEdge.getStatement() instanceof CAssignment assignment) {
      Type type = assignment.getLeftHandSide().getExpressionType().getCanonicalType();
      if (!(type instanceof CPointerType)) {
        return pState;
      }
      if (assignment instanceof CFunctionCallAssignmentStatement callAssignment) {
        CExpression lhs = callAssignment.getLeftHandSide();
        if (PointerAnalysisChecks.isNondetPointerReturn(
            callAssignment.getFunctionCallExpression().getFunctionNameExpression())) {
          // if (isNondetPointerReturn(rhs.getFunctionNameExpression())) {
          // We don't consider summary edges, so if we encounter a function call assignment edge,
          // this means that the called function is not defined.
          // If the function returns a non-deterministic pointer,
          // handle it that way.
          // Do not add to pointsToMap, since ⊤ is not explicitly tracked in this implementation.
          // By default, all pointers that are not present in pointsToMap are assumed to point to
          // Top.
          return pState;
        }
        if (PointerAnalysisChecks.isMallocFunction(
            callAssignment.getFunctionCallExpression().getFunctionNameExpression())) {
          HeapLocation heapLocation;

          heapLocation = createHeapLocation(pCfaEdge);

          LocationSet lhsLocations = getReferencedLocations(lhs, pState, false, pCfaEdge, options);
          LocationSet rhsSet = LocationSetFactory.withPointerLocation(heapLocation);

          return handleAssignment(pState, lhsLocations, rhsSet, pCfaEdge);
        }
      }

      return handleAssignment(
          pState, assignment.getLeftHandSide(), assignment.getRightHandSide(), pCfaEdge);
    }
    return pState;
  }

  public PointerAnalysisState handleDeallocation(
      PointerAnalysisState pState, CStatementEdge pCfaEdge, CFunctionCallExpression callExpr)
      throws CPATransferException {
    CExpression freedExpr = callExpr.getParameterExpressions().get(0);

    LocationSet targets = getReferencedLocations(freedExpr, pState, true, pCfaEdge, options);

    if (targets instanceof ExplicitLocationSet explicitTargets) {
      PersistentMap<PointerLocation, LocationSet> newPointsToMap = pState.getPointsToMap();
      for (PointerLocation pt : explicitTargets.sortedPointerLocations()) {
        if (pt instanceof HeapLocation) {
          PointerLocation invalid = new InvalidLocation(InvalidationReason.FREED);
          newPointsToMap =
              newPointsToMap.putAndCopy(pt, LocationSetFactory.withPointerLocation(invalid));
        } else {
          logger.logf(
              Level.WARNING,
              "free() called on non-heap object: %s at %s",
              pt,
              pCfaEdge.getFileLocation());
        }
      }
      return new PointerAnalysisState(newPointsToMap);
    }
    return pState;
  }

  private HeapLocation createHeapLocation(CCfaEdge pCfaEdge) {
    String functionName = pCfaEdge.getPredecessor().getFunctionName();
    HeapLocation heapLocation;
    switch (options.getHeapAllocationStrategy()) {
      case SINGLE -> heapLocation = HeapLocation.forSingleAllocation(functionName, null);
      case PER_CALL ->
          heapLocation =
              HeapLocation.forIndexedAllocation(
                  functionName, allocationCounter.getAndIncrement(), 0L);
      case PER_LINE -> {
        int line = pCfaEdge.getFileLocation().getStartingLineInOrigin();
        heapLocation = HeapLocation.forLineBasedAllocation(functionName, line, 0L);
      }
      default ->
          throw new AssertionError(
              "Unhandled heap allocation strategy: " + options.getHeapAllocationStrategy());
    }
    return heapLocation;
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

  private PointerAnalysisState handleAssignment(
      PointerAnalysisState pState, CExpression pLhs, CRightHandSide pRhs, CCfaEdge pCfaEdge)
      throws CPATransferException {

    LocationSet lhsLocations = getReferencedLocations(pLhs, pState, false, pCfaEdge, options);
    LocationSet rhsTargets = getReferencedLocations(pRhs, pState, true, pCfaEdge, options);
    if (pLhs instanceof CFieldReference pCFieldReference) {
      CType baseType = pCFieldReference.getFieldOwner().getExpressionType().getCanonicalType();

      while (baseType instanceof CPointerType ptrType) {
        baseType = ptrType.getType().getCanonicalType();

        if (StructUnionAssignmentHandler.isUnion(baseType)
            || StructUnionAssignmentHandler.isStruct(baseType)) {
          return StructUnionAssignmentHandler.handleAssignmentForStructOrUnionType(
              pState,
              baseType,
              lhsLocations,
              rhsTargets,
              pCfaEdge,
              options.getStructHandlingStrategy(),
              logger);
        }
      }
    }
    return handleAssignment(pState, lhsLocations, rhsTargets, pCfaEdge);
  }
}
