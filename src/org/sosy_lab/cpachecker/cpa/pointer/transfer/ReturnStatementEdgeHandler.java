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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.utils.PointerAnalysisChecks;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class ReturnStatementEdgeHandler
    implements TransferRelationEdgeHandler<CReturnStatementEdge> {

  private final PointerTransferOptions options;

  public ReturnStatementEdgeHandler(PointerTransferOptions pOptions) {
    options = pOptions;
  }

  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    Optional<CExpression> expression = pCfaEdge.getExpression();
    if (expression.isEmpty()) {
      return pState;
    }
    CExpression returnExpression = expression.orElseThrow();
    Type returnType = returnExpression.getExpressionType();
    if (!(returnType instanceof CPointerType)) {
      return pState;
    }
    LocationSet returnLocations =
        getReferencedLocations(returnExpression, pState, true, pCfaEdge, options);
    if (returnLocations.isTop()) {
      return pState;
    }
    Optional<MemoryLocation> returnVariable =
        PointerAnalysisChecks.getFunctionReturnVariable(pCfaEdge.getSuccessor().getEntryNode());

    return returnVariable
        .map(
            memoryLocation ->
                new PointerAnalysisState(
                    pState
                        .getPointsToMap()
                        .putAndCopy(new DeclaredVariableLocation(memoryLocation), returnLocations)))
        .orElse(pState);
  }
}
