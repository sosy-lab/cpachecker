/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.variableclassification;

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
 * expression is a var or a simple mathematical calculation (add, sub, <, >, <=, >=, ==, !=, !), -
 * else null
 */
class IntAddCollectingVisitor extends VariablesCollectingVisitor {

  private final Set<String> nonIntAddVars;

  public IntAddCollectingVisitor(CFANode pre, Set<String> pNonIntAddVars) {
    super(pre);
    nonIntAddVars = pNonIntAddVars;
  }

  @Override
  public Set<String> visit(CCastExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CFieldReference exp) {
    nonIntAddVars.addAll(super.visit(exp));
    return null;
  }

  @Override
  public Set<String> visit(CBinaryExpression exp) {
    Set<String> operand1 = exp.getOperand1().accept(this);
    Set<String> operand2 = exp.getOperand2().accept(this);

    if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
      if (operand1 != null) {
        nonIntAddVars.addAll(operand1);
      }
      if (operand2 != null) {
        nonIntAddVars.addAll(operand2);
      }
      return null;
    }

    switch (exp.getOperator()) {
      case PLUS:
      case MINUS:
      case LESS_THAN:
      case LESS_EQUAL:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case EQUALS:
      case NOT_EQUALS:
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
        // this calculations work with all numbers
        operand1.addAll(operand2);
        return operand1;

      default: // *, /, %, shift --> no simple calculations
        nonIntAddVars.addAll(operand1);
        nonIntAddVars.addAll(operand2);
        return null;
    }
  }

  @Override
  public Set<String> visit(CIntegerLiteralExpression exp) {
    return new HashSet<>(0);
  }

  @Override
  public Set<String> visit(CUnaryExpression exp) {
    Set<String> inner = exp.getOperand().accept(this);
    if (inner == null) {
      return null;
    }
    if (exp.getOperator() == UnaryOperator.MINUS) {
      return inner;
    }

    // *, ~, etc --> not simple
    nonIntAddVars.addAll(inner);
    return null;
  }

  @Override
  public Set<String> visit(CPointerExpression exp) {
    Set<String> inner = exp.getOperand().accept(this);
    if (inner == null) {
      return null;
    }

    nonIntAddVars.addAll(inner);
    return null;
  }
}
