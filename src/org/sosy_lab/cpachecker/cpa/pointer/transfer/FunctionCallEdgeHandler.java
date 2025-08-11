// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import static org.sosy_lab.cpachecker.cpa.pointer.utils.ReferenceLocationsResolver.getReferencedLocations;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Handles transitions into a callee. It maps actual pointer arguments at the call site to the
 * callee’s formal parameters. The handler updates the state so parameter pointers refer to the same
 * targets as the passed arguments.
 */
public final class FunctionCallEdgeHandler
    implements TransferRelationEdgeHandler<CFunctionCallEdge> {
  private PointerTransferOptions options;

  public FunctionCallEdgeHandler(PointerTransferOptions pOptions) {
    options = pOptions;
  }

  @Override
  public PointerAnalysisState handleEdge(
      PointerAnalysisState pState, CFunctionCallEdge pCFunctionCallEdge)
      throws CPATransferException {

    PointerAnalysisState newState = pState;

    List<CParameterDeclaration> formalParameters =
        pCFunctionCallEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pCFunctionCallEdge.getArguments();

    Preconditions.checkState(actualParams.size() == formalParameters.size());

    int numberOfParameters = actualParams.size();

    formalParameters = FluentIterable.from(formalParameters).limit(numberOfParameters).toList();
    actualParams = FluentIterable.from(actualParams).limit(numberOfParameters).toList();

    for (Pair<CParameterDeclaration, CExpression> param :
        Pair.zipList(formalParameters, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration formalParam = param.getFirst();

      if (!(Objects.requireNonNull(formalParam).getType().getCanonicalType()
          instanceof CPointerType)) {
        continue;
      }

      DeclaredVariableLocation paramLocationPointer =
          new DeclaredVariableLocation(DeclaredVariableLocation.getMemoryLocation(formalParam));
      LocationSet referencedLocations =
          getReferencedLocations(
              Objects.requireNonNull(actualParam), pState, true, pCFunctionCallEdge, options);

      newState =
          new PointerAnalysisState(
              newState.getPointsToMap().putAndCopy(paramLocationPointer, referencedLocations));
    }

    for (CParameterDeclaration formalParam :
        FluentIterable.from(formalParameters).skip(numberOfParameters)) {
      DeclaredVariableLocation paramLocationPointer =
          new DeclaredVariableLocation(DeclaredVariableLocation.getMemoryLocation(formalParam));
      newState =
          new PointerAnalysisState(
              newState
                  .getPointsToMap()
                  .putAndCopy(paramLocationPointer, LocationSetFactory.withBot()));
    }

    return newState;
  }
}
