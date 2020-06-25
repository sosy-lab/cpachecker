// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This Visitor evaluates an Expression. Each visit-function returns - a collection, if the
 * expression is an arithmetic calculation where an overflow can occur (+ , -, *, /, %, <<) else
 * null
 */
public class IntOverflowCollectingVisitor extends VariablesCollectingVisitor {

  private final Set<String> intOverflowsVars;

  public IntOverflowCollectingVisitor(CFANode pre, Set<String> pIntOverflowVars) {
    super(pre);
    intOverflowsVars = checkNotNull(pIntOverflowVars);
  }

  @Override
  public Set<String> visit(CCastExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CFieldReference exp) {
    return super.visit(exp);
  }

  @Override
  public Set<String> visit(CBinaryExpression exp) {
    Set<String> operand1 = exp.getOperand1().accept(this);
    Set<String> operand2 = exp.getOperand2().accept(this);

    switch (exp.getOperator()) {
      case PLUS:
      case MINUS:
      case MULTIPLY:
      case DIVIDE:
      case MODULO:
      case SHIFT_LEFT:
        // here an overflow can occur
        if (operand1 != null) {
          intOverflowsVars.addAll(operand1);
        }
        if (operand2 != null) {
          intOverflowsVars.addAll(operand2);
        }
        return null;

      default: // no overflow can occur
        if (operand1 == null || operand2 == null) {
          if (operand1 != null) {
            return operand1;
          }
          if (operand2 != null) {
            return operand2;
          }
          return null;
        } else {
          operand1.addAll(operand2);
          return operand1;
        }
    }
  }

  @Override
  public Set<String> visit(CIntegerLiteralExpression exp) {
    checkNotNull(exp);
    return new HashSet<>(0);
  }

  @Override
  public Set<String> visit(CUnaryExpression exp) {
    Set<String> inner = exp.getOperand().accept(this);
    if (inner == null) {
      return null;
    }
    if (exp.getOperator() == UnaryOperator.MINUS) {
      intOverflowsVars.addAll(inner);
      return null;
    }

    // *, ~, etc --> not simple
    return inner;
  }

  @Override
  public Set<String> visit(CPointerExpression exp) {
    return exp.getOperand().accept(this);
  }
}
