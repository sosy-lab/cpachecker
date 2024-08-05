// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This Visitor evaluates an Expression. It also collects all variables. Each visit-function returns
 * - null, if the expression is not boolean - a collection, if the expression is boolean. The
 * collection contains all boolean vars.
 */
class BoolCollectingVisitor extends VariablesCollectingVisitor {

  /**
   * normally a boolean value would be 0 or 1, however there are cases, where the values are only 0
   * and 1, but the variable is not boolean at all: "int x; if(x!=0 && x!= 1){}". so we allow only 0
   * as boolean value, and not 1.
   */
  private static boolean allowOneAsBooleanValue = false;

  private final Set<String> nonIntBoolVars;

  public BoolCollectingVisitor(CFANode pre, Set<String> pNonIntBoolVars) {
    super(pre);
    nonIntBoolVars = checkNotNull(pNonIntBoolVars);
  }

  @Override
  public Set<String> visit(CFieldReference exp) {
    nonIntBoolVars.addAll(super.visit(exp));
    return null;
  }

  @Override
  public Set<String> visit(CBinaryExpression exp) {
    Set<String> operand1 = exp.getOperand1().accept(this);
    Set<String> operand2 = exp.getOperand2().accept(this);

    if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
      if (operand1 != null) {
        nonIntBoolVars.addAll(operand1);
      }
      if (operand2 != null) {
        nonIntBoolVars.addAll(operand2);
      }
      return null;
    }

    switch (exp.getOperator()) {
      case EQUALS:
      case NOT_EQUALS: // ==, != work with boolean operands
        if (operand1.isEmpty() || operand2.isEmpty()) {
          // one operand is Zero (or One, if allowed)
          operand1.addAll(operand2);
          return operand1;
        }
        // We compare 2 variables. There is no guarantee, that they are boolean!
        // Example: (a!=b) && (b!=c) && (c!=a)
        // -> FALSE for boolean, but TRUE for {1,2,3}

        // $FALL-THROUGH$

      default: // +-*/ --> no boolean operators, a+b --> a and b are not boolean
        nonIntBoolVars.addAll(operand1);
        nonIntBoolVars.addAll(operand2);
        return null;
    }
  }

  @Override
  public Set<String> visit(CIntegerLiteralExpression exp) {
    BigInteger value = exp.getValue();
    if (BigInteger.ZERO.equals(value) || (allowOneAsBooleanValue && BigInteger.ONE.equals(value))) {
      return new HashSet<>(0);
    } else {
      return null;
    }
  }

  @Override
  public Set<String> visit(CUnaryExpression exp) {
    Set<String> inner = exp.getOperand().accept(this);

    if (inner == null) {
      return null;
    } else { // PLUS, MINUS, etc --> not boolean
      nonIntBoolVars.addAll(inner);
      return null;
    }
  }

  @Override
  public Set<String> visit(CPointerExpression exp) {
    Set<String> inner = exp.getOperand().accept(this);

    if (inner == null) {
      return null;
    } else {
      nonIntBoolVars.addAll(inner);
      return null;
    }
  }
}
