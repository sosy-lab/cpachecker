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
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This Visitor evaluates an Expression. Each visit-function returns - null, if the expression
 * contains calculations - a collection, if the expression is a number, unaryExp, == or !=
 */
class IntEqualCollectingVisitor extends VariablesCollectingVisitor {

  private final Set<String> nonIntEqVars;

  public IntEqualCollectingVisitor(CFANode pre, Set<String> pNonIntEqVars) {
    super(pre);
    nonIntEqVars = checkNotNull(pNonIntEqVars);
  }

  @Override
  public Set<String> visit(CCastExpression exp) {
    BigInteger val = VariableClassificationBuilder.getNumber(exp.getOperand());
    if (val == null) {
      return exp.getOperand().accept(this);
    } else {
      return new HashSet<>(0);
    }
  }

  @Override
  public Set<String> visit(CFieldReference exp) {
    nonIntEqVars.addAll(super.visit(exp));
    return null;
  }

  @Override
  public Set<String> visit(CBinaryExpression exp) {

    // for numeral values
    BigInteger val1 = VariableClassificationBuilder.getNumber(exp.getOperand1());
    Set<String> operand1;
    if (val1 == null) {
      operand1 = exp.getOperand1().accept(this);
    } else {
      operand1 = new HashSet<>(0);
    }

    // for numeral values
    BigInteger val2 = VariableClassificationBuilder.getNumber(exp.getOperand2());
    Set<String> operand2;
    if (val2 == null) {
      operand2 = exp.getOperand2().accept(this);
    } else {
      operand2 = new HashSet<>(0);
    }

    // handle vars from operands
    if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
      if (operand1 != null) {
        nonIntEqVars.addAll(operand1);
      }
      if (operand2 != null) {
        nonIntEqVars.addAll(operand2);
      }
      return null;
    }

    switch (exp.getOperator()) {
      case EQUALS:
      case NOT_EQUALS: // ==, != work with numbers
        operand1.addAll(operand2);
        return operand1;

      default: // +-*/ --> no simple operators
        nonIntEqVars.addAll(operand1);
        nonIntEqVars.addAll(operand2);
        return null;
    }
  }

  @Override
  public Set<String> visit(CIntegerLiteralExpression exp) {
    checkNotNull(exp);
    return new HashSet<>(0);
  }

  @Override
  public Set<String> visit(CUnaryExpression exp) {

    // if exp is numeral
    BigInteger val = VariableClassificationBuilder.getNumber(exp);
    if (val != null) {
      return new HashSet<>(0);
    }

    // if exp is binary expression
    Set<String> inner = exp.getOperand().accept(this);
    if (isNestedBinaryExp(exp)) {
      return inner;
    }

    if (inner != null) {
      nonIntEqVars.addAll(inner);
    }
    return null;
  }

  @Override
  public Set<String> visit(CPointerExpression exp) {

    // if exp is numeral
    BigInteger val = VariableClassificationBuilder.getNumber(exp);
    if (val != null) {
      return new HashSet<>(0);
    }

    // if exp is binary expression
    Set<String> inner = exp.getOperand().accept(this);
    if (isNestedBinaryExp(exp)) {
      return inner;
    }

    // if exp is unknown
    if (inner == null) {
      return null;
    }

    nonIntEqVars.addAll(inner);
    return null;
  }

  /** returns true, if the expression contains a casted binaryExpression. */
  private static boolean isNestedBinaryExp(CExpression exp) {
    if (exp instanceof CBinaryExpression) {
      return true;

    } else if (exp instanceof CCastExpression) {
      return isNestedBinaryExp(((CCastExpression) exp).getOperand());

    } else {
      return false;
    }
  }
}
