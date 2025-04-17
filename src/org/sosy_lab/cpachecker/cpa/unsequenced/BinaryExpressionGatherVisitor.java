// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BinaryExpressionGatherVisitor extends DefaultCExpressionVisitor<Set<CBinaryExpression>, UnrecognizedCodeException> {

  private final LogManager logger;

  public BinaryExpressionGatherVisitor(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  protected Set<CBinaryExpression> visitDefault(CExpression exp) throws UnrecognizedCodeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CBinaryExpression> visit(CBinaryExpression expr) throws UnrecognizedCodeException {
    Set<CBinaryExpression> binaryExprs = new HashSet<>();

    binaryExprs.addAll(expr.getOperand1().accept(this));
    binaryExprs.addAll(expr.getOperand2().accept(this));

    if (isUnsequencedBinaryOperator(expr.getOperator())) {
      binaryExprs.add(expr);
    }

    logger.log(
        Level.INFO,
        String.format("Detected unsequenced binary expression '%s' at %s",
            expr.toASTString(),
            expr.getFileLocation())
    );

    return binaryExprs;
  }

  private boolean isUnsequencedBinaryOperator(CBinaryExpression.BinaryOperator op) {
    return switch (op) {
      case BINARY_AND, BINARY_OR -> false;
      case MULTIPLY, DIVIDE, MODULO,
           PLUS, MINUS,
           SHIFT_LEFT, SHIFT_RIGHT, BINARY_XOR,
           LESS_EQUAL, LESS_THAN, GREATER_EQUAL, GREATER_THAN,
           EQUALS, NOT_EQUALS -> true;
      default -> throw new AssertionError("Unhandled operator in isUnsequencedBinaryOperator: " + op);
    };
  }

}
