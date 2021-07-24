// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.Sets;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions.GenericAssumptionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


public final class ArithmeticOverflowAssumptionBuilder extends ArithmeticAssumptionBuilder
    implements GenericAssumptionBuilder {


  public ArithmeticOverflowAssumptionBuilder(
      CFA cfa,
      LogManager logger,
      Configuration pConfiguration)
      throws InvalidConfigurationException {
    super(cfa, logger, pConfiguration);

  }

  public ArithmeticOverflowAssumptionBuilder(
      MachineModel pMachineModel,
      Optional<LiveVariables> pLiveVariables,
      LogManager logger,
      Configuration pConfiguration)
      throws InvalidConfigurationException {
    super(pMachineModel, pLiveVariables, logger, pConfiguration);
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

      Set<ASimpleDeclaration> liveVars =
          liveVariables.orElseThrow().getLiveVariablesForNode(node);
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
        if (upperBounds.get(calculationType) != null) {
          result.add(
              ofmgr.getUpperAssumption(op1, op2, binop, upperBounds.get(calculationType)));
        }
      } else if (trackMultiplications && binop.equals(BinaryOperator.MULTIPLY)) {
        if (upperBounds.get(calculationType) != null) {
          result.addAll(
              ofmgr.addMultiplicationAssumptions(
                  op1,
                  op2,
                  upperBounds.get(calculationType),
                  upperBounds.get(calculationType)));
        }

      } else if (trackLeftShifts && binop.equals(BinaryOperator.SHIFT_LEFT)) {
        if (upperBounds.get(calculationType) != null && width.get(calculationType) != null) {
          ofmgr.addLeftShiftAssumptions(op1, op2, upperBounds.get(calculationType), result);
        }
      }

    } else {
      // TODO: check out and implement in case this happens
    }

  }

}
