// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.visitor;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.PartialState.ApplyEpsilon;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.PartialState.TruthAssumption;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NumericAssumptionHandler
    implements CExpressionVisitor<Collection<NumericState>, UnrecognizedCodeException> {

  private final NumericState state;
  private final TruthAssumption truthAssumption;
  private final LogManager logger;

  public NumericAssumptionHandler(
      NumericState pState, boolean pTruthAssumption, LogManager logManager) {
    state = pState;
    logger = logManager;
    if (pTruthAssumption) {
      truthAssumption = TruthAssumption.ASSUME_TRUE;
    } else {
      truthAssumption = TruthAssumption.ASSUME_FALSE;
    }
  }

  @Override
  public Collection<NumericState> visit(CBinaryExpression pIastBinaryExpression)
      throws UnrecognizedCodeException {
    if (logger.wouldBeLogged(Level.FINEST)) {
      logger.log(
          Level.FINEST,
          "Assumption: ",
          truthAssumption,
          pIastBinaryExpression.toQualifiedASTString());
    }

    Collection<PartialState> statesLeft =
        pIastBinaryExpression
            .getOperand1()
            .accept(new NumericRightHandSideVisitor(state.getValue().getEnvironment(), null));
    Collection<PartialState> statesRight =
        pIastBinaryExpression
            .getOperand2()
            .accept(new NumericRightHandSideVisitor(state.getValue().getEnvironment(), null));

    Collection<PartialState> states;

    if (checkIsFloatComparison(pIastBinaryExpression)) {
      // Use comparison with epsilon for real valued variables
      states =
          PartialState.applyComparisonOperator(
              pIastBinaryExpression.getOperator(),
              statesLeft,
              statesRight,
              truthAssumption,
              ApplyEpsilon.APPLY_EPSILON,
              state.getValue().getEnvironment());
    } else {
      states =
          PartialState.applyComparisonOperator(
              pIastBinaryExpression.getOperator(),
              statesLeft,
              statesRight,
              truthAssumption,
              ApplyEpsilon.EXACT,
              state.getValue().getEnvironment());
    }

    ImmutableSet.Builder<NumericState> successorsBuilder = new ImmutableSet.Builder<>();

    for (PartialState partialState : states) {
      Optional<NumericState> successor = state.meet(partialState.getConstraints());
      successor.ifPresent(successorsBuilder::add);
    }

    Collection<NumericState> successors = successorsBuilder.build();
    return NumericTransferRelation.removeEmptyStates(successors);
  }

  @Override
  public Collection<NumericState> visit(CCastExpression pIastCastExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CTypeIdExpression pIastTypeIdExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CUnaryExpression pIastUnaryExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CFieldReference pIastFieldReference)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CIdExpression pIastIdExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CPointerExpression pointerExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(CComplexCastExpression complexCastExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  private boolean checkIsFloatComparison(CBinaryExpression pIastBinaryExpression) {
    CSimpleType typeOperand1 =
        (CSimpleType) pIastBinaryExpression.getOperand1().getExpressionType();
    CSimpleType typeOperand2 =
        (CSimpleType) pIastBinaryExpression.getOperand2().getExpressionType();
    return (typeOperand1.getType().isFloatingPointType()
        || typeOperand2.getType().isFloatingPointType());
  }
}
