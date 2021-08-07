// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions.GenericAssumptionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class ArithmeticUnderflowAssumptionBuilder extends ArithmeticAssumptionBuilder
    implements GenericAssumptionBuilder {

  public ArithmeticUnderflowAssumptionBuilder(
      CFA cfa,
      LogManager logger,
      Configuration pConfiguration)
      throws InvalidConfigurationException {
    super(cfa, logger, pConfiguration);
  }

  /**
   * Compute assumptions whose conjunction states that the expression does not overflow the allowed
   * bound of its type.
   */
  @Override
  void addAssumptionOnBounds(CExpression exp, Set<CExpression> result, CFANode node)
      throws UnrecognizedCodeException {
    if (useLiveness) {
      Set<CSimpleDeclaration> referencedDeclarations =
          CFAUtils.getIdExpressionsOfExpression(exp)
              .transform(CIdExpression::getDeclaration)
              .toSet();

      Set<ASimpleDeclaration> liveVars = liveVariables.orElseThrow().getLiveVariablesForNode(node);
      if (Sets.intersection(referencedDeclarations, liveVars).isEmpty()) {
        logger.log(Level.FINE, "No live variables found in expression", exp, "skipping");
        return;
      }
    }

    if (isBinaryExpressionThatMayOverflow(exp)) {
      CBinaryExpression binexp = (CBinaryExpression) exp;
      BinaryOperator binop = binexp.getOperator();
      CType calculationType = binexp.getCalculationType();
      CExpression op1 = binexp.getOperand1();
      CExpression op2 = binexp.getOperand2();
      if (trackAdditiveOperations
          && (binop.equals(BinaryOperator.PLUS) || binop.equals(BinaryOperator.MINUS))) {
        if (lowerBounds.get(calculationType) != null) {
          result.add(ufmgr.getLowerAssumption(op1, op2, binop, lowerBounds.get(calculationType)));
        }
      } else if (trackMultiplications && binop.equals(BinaryOperator.MULTIPLY)) {
        if (lowerBounds.containsKey(calculationType) && upperBounds.containsKey(calculationType)) {
          result.addAll(
              ufmgr.addMultiplicationAssumptions(
                  op1,
                  op2,
                  lowerBounds.get(calculationType),
                  upperBounds.get(calculationType)));
        }

      } else if (trackDivisions
          && (binop.equals(BinaryOperator.DIVIDE) || binop.equals(BinaryOperator.MODULO))) {
        if (lowerBounds.get(calculationType) != null) {
          ufmgr.addDivisionAssumption(op1, op2, lowerBounds.get(calculationType), result);
        }
      }
    } else if (exp instanceof CUnaryExpression) {
      CType calculationType = exp.getExpressionType();
      CUnaryExpression unaryexp = (CUnaryExpression) exp;
      if (unaryexp.getOperator().equals(CUnaryExpression.UnaryOperator.MINUS)
          && lowerBounds.get(calculationType) != null) {

        CExpression operand = unaryexp.getOperand();
        result.add(ufmgr.getNegationAssumption(operand, lowerBounds.get(calculationType)));
      }
    } else {
      // TODO: check out and implement in case this happens
    }

  }


}
