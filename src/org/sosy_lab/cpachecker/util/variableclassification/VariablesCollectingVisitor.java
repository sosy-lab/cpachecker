// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Set;
import java.util.TreeSet;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * This Visitor evaluates an Expression. It collects all variables. a visit of IdExpression or
 * CFieldReference returns a collection containing the varName, other visits return the inner
 * visit-results. The Visitor also collects all numbers used in the expression.
 */
class VariablesCollectingVisitor implements CExpressionVisitor<Set<String>, NoException> {

  private CFANode predecessor;
  private Set<BigInteger> values = new TreeSet<>();

  public VariablesCollectingVisitor(CFANode pre) {
    predecessor = checkNotNull(pre);
  }

  public Set<BigInteger> getValues() {
    return values;
  }

  @Override
  public Set<String> visit(CArraySubscriptExpression exp) {
    checkNotNull(exp);
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
      values.add(val1);
      operand1 = null;
    }

    // for numeral values
    BigInteger val2 = VariableClassificationBuilder.getNumber(exp.getOperand2());
    Set<String> operand2;
    if (val2 == null) {
      operand2 = exp.getOperand2().accept(this);
    } else {
      values.add(val2);
      operand2 = null;
    }

    // handle vars from operands
    if (operand1 == null) {
      return operand2;
    } else if (operand2 == null) {
      return operand1;
    } else {
      operand1.addAll(operand2);
      return operand1;
    }
  }

  @Override
  public Set<String> visit(CCastExpression exp) {
    BigInteger val = VariableClassificationBuilder.getNumber(exp.getOperand());
    if (val == null) {
      return exp.getOperand().accept(this);
    } else {
      values.add(val);
      return null;
    }
  }

  @Override
  public Set<String> visit(CComplexCastExpression exp) {
    // TODO complex numbers are not supported for evaluation right now, this
    // way of handling the variables my be wrong

    BigInteger val = VariableClassificationBuilder.getNumber(exp.getOperand());
    if (val == null) {
      return exp.getOperand().accept(this);
    } else {
      values.add(val);
      return null;
    }
  }

  @Override
  public Set<String> visit(CFieldReference exp) {
    String varName = exp.toASTString(); // TODO "(*p).x" vs "p->x"
    String function =
        VariableClassificationBuilder.isGlobal(exp) ? "" : predecessor.getFunctionName();
    Set<String> ret = Sets.newHashSetWithExpectedSize(1);
    ret.add(VariableClassificationBuilder.scopeVar(function, varName));
    return ret;
  }

  @Override
  public Set<String> visit(CIdExpression exp) {
    Set<String> ret = Sets.newHashSetWithExpectedSize(1);
    ret.add(exp.getDeclaration().getQualifiedName());
    return ret;
  }

  @Override
  public Set<String> visit(CCharLiteralExpression exp) {
    checkNotNull(exp);
    return null;
  }

  @Override
  public Set<String> visit(CFloatLiteralExpression exp) {
    checkNotNull(exp);
    return null;
  }

  @Override
  public Set<String> visit(CImaginaryLiteralExpression exp) {
    return exp.getValue().accept(this);
  }

  @Override
  public Set<String> visit(CIntegerLiteralExpression exp) {
    values.add(exp.getValue());
    return null;
  }

  @Override
  public Set<String> visit(CStringLiteralExpression exp) {
    checkNotNull(exp);
    return null;
  }

  @Override
  public Set<String> visit(CTypeIdExpression exp) {
    checkNotNull(exp);
    return null;
  }

  @Override
  public Set<String> visit(CUnaryExpression exp) {
    BigInteger val = VariableClassificationBuilder.getNumber(exp);
    if (val == null) {
      return exp.getOperand().accept(this);
    } else {
      values.add(val);
      return null;
    }
  }

  @Override
  public Set<String> visit(CPointerExpression exp) {
    BigInteger val = VariableClassificationBuilder.getNumber(exp);
    if (val == null) {
      return exp.getOperand().accept(this);
    } else {
      values.add(val);
      return null;
    }
  }

  @Override
  public Set<String> visit(CAddressOfLabelExpression exp) {
    checkNotNull(exp);
    return null;
  }
}
