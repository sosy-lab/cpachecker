// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import static org.sosy_lab.cpachecker.cpa.pointer.utils.ReferenceLocationsResolver.getReferencedLocations;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.utils.PointerAnalysisChecks;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Processes AssumeEdge conditions (e.g., if statements) to refine pointer state based on pointer
 * equality/inequality and null-comparisons. It narrows possible points-to sets or detects
 * contradictions that make the state infeasible (returning BOTTOM_STATE).
 */
public final class AssumeEdgeHandler implements TransferRelationEdgeHandler<AssumeEdge> {
  private final PointerTransferOptions pOptions;

  public AssumeEdgeHandler(PointerTransferOptions options) {
    this.pOptions = options;
  }

  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, AssumeEdge pAssumeEdge)
      throws CPATransferException {
    if (!(pAssumeEdge.getExpression() instanceof CExpression condition)) {
      return pState;
    }

    boolean truthAssumption = pAssumeEdge.getTruthAssumption();

    if (condition instanceof CBinaryExpression binaryExpression) {
      return handleBasicBinaryCondition(pState, binaryExpression, truthAssumption, pAssumeEdge);
    }

    return pState;
  }

  private PointerAnalysisState handleBasicBinaryCondition(
      PointerAnalysisState pState,
      CBinaryExpression pExpression,
      boolean pTruthAssumption,
      AssumeEdge pCFAEdge)
      throws CPATransferException {

    boolean isEqualsOperator = pExpression.getOperator() == CBinaryExpression.BinaryOperator.EQUALS;
    boolean isNotEqualsOperator =
        pExpression.getOperator() == CBinaryExpression.BinaryOperator.NOT_EQUALS;

    if (isEqualsOperator || isNotEqualsOperator) {

      CExpression leftOperand = pExpression.getOperand1();
      CExpression rightOperand = pExpression.getOperand2();

      Type typeLeftOperand = leftOperand.getExpressionType().getCanonicalType();
      Type typeRightOperand = rightOperand.getExpressionType().getCanonicalType();

      boolean isNullComparison =
          PointerAnalysisChecks.isNullPointer(leftOperand)
              || PointerAnalysisChecks.isNullPointer(rightOperand);

      boolean leftNotPointer = !(typeLeftOperand instanceof CPointerType);
      boolean rightNotPointer = !(typeRightOperand instanceof CPointerType);
      boolean noPointersInExpr = leftNotPointer && rightNotPointer;
      boolean bothOperandsShouldBePointers = !isNullComparison;
      boolean atLeastOneOperandIsNotPointer = leftNotPointer || rightNotPointer;

      if ((bothOperandsShouldBePointers && atLeastOneOperandIsNotPointer)
          || (isNullComparison && noPointersInExpr)) {
        return pState;
      }

      if (isNullComparison) {
        CExpression pointerExpr =
            PointerAnalysisChecks.isNullPointer(leftOperand) ? rightOperand : leftOperand;
        LocationSet pointsTo =
            getReferencedLocations(pointerExpr, pState, true, pCFAEdge, pOptions);

        if (pointsTo.isTop()) {
          return pState;
        }
        if (pointsTo instanceof ExplicitLocationSet explicitPointsTo) {
          boolean mustBeEqualToNull = (isEqualsOperator == pTruthAssumption);

          if ((mustBeEqualToNull && explicitPointsTo.containsAnyNull())
              || (!mustBeEqualToNull && !explicitPointsTo.containsAllNulls())) {
            return pState;
          }
        }
        return PointerAnalysisState.BOTTOM_STATE;
      } else {
        LocationSet leftPointsTo =
            getReferencedLocations(leftOperand, pState, true, pCFAEdge, pOptions);
        LocationSet rightPointsTo =
            getReferencedLocations(rightOperand, pState, true, pCFAEdge, pOptions);

        if (leftPointsTo.isTop() || rightPointsTo.isTop()) {
          return pState;
        }

        if (leftPointsTo.isBot() && rightPointsTo.isBot()) {
          return PointerAnalysisState.BOTTOM_STATE;
        }

        if (leftPointsTo.isBot() || rightPointsTo.isBot()) {
          boolean mustBeEqual = (isEqualsOperator == pTruthAssumption);
          if (mustBeEqual) {
            return PointerAnalysisState.BOTTOM_STATE;
          } else {
            return pState;
          }
        }

        boolean mustBeEqual = (isEqualsOperator == pTruthAssumption);

        if (mustBeEqual) {
          if (leftPointsTo instanceof ExplicitLocationSet explicitLeftPointsTo
              && rightPointsTo instanceof ExplicitLocationSet explicitRightPointsTo) {

            if (explicitLeftPointsTo.equals(explicitRightPointsTo)) {
              return pState;
            }
            if (!explicitLeftPointsTo.hasCommonLocation(explicitRightPointsTo)) {
              return PointerAnalysisState.BOTTOM_STATE;
            }
          }
        } else {
          if (leftPointsTo instanceof ExplicitLocationSet explicitLeftPointsTo
              && rightPointsTo instanceof ExplicitLocationSet explicitRightPointsTo) {

            if (explicitLeftPointsTo.equals(explicitRightPointsTo)) {
              return PointerAnalysisState.BOTTOM_STATE;
            }
          }
        }
        return pState;
      }
    }
    return pState;
  }
}
