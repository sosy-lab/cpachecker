// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExpressionNode implements FormulaNode {

  private String operator;
  private boolean negated;
  private List<FormulaNode> operands;

  public ExpressionNode(String pOperator) {
    operator = pOperator;
    operands = new ArrayList<>();
    negated = false;
  }

  public void negateOperator() {
    negated = !negated;
  }

  @Override
  public List<FormulaNode> getSuccessors() {
    return ImmutableList.of();
  }

  @Override
  public FormulaNodeType getType() {
    return FormulaNodeType.ExpressionNode;
  }

  public void addOperand(ExpressionNode node) {
    operands.add(node);
  }

  public List<FormulaNode> getOperands() {
    return operands;
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof ExpressionNode)) {
      return false;
    }
    ExpressionNode that = (ExpressionNode) pO;
    if (operands.size() != that.operands.size()) {
      return false;
    }
    for (int i = 0; i < operands.size(); i++) {
      if (!operands.get(i).equals(that.operands.get(i))) {
        return false;
      }
    }
    return Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    int hashCode = Objects.hash(ExpressionNode.class, operator);
    for (FormulaNode operand : operands) {
      hashCode = Objects.hash(hashCode, operand);
    }
    return hashCode;
  }

  @Override
  public String toString() {
    String op = readableOperator(operator, negated);
    if (operands.size() == 2) {
      return "(" + operands.get(0) + " " + op + " "  + operands.get(1) + ")";
    }
    return "(" + op + " " + operands.stream().map(opt -> opt.toString()).collect(Collectors.joining(" ")) + ")";
  }

  private String readableOperator (String pOperator, boolean pNegated) {
    String copy = pOperator;
    if (pOperator.contains("_") && !pOperator.startsWith("_")) {
      pOperator = Splitter.on("_").splitToList(pOperator).get(0);
    }
    switch (pOperator) {
      case "=":
        return pNegated ? "≠" : "=";
      case "bvsdiv":
        return "/";
      case "bvadd":
        return "+";
      case "bvslt":
        return pNegated ? "≥" : "<";
      case "bvextract":
        return copy;
      default:
        return pNegated ? "!" + pOperator : pOperator;
    }

  }

  public void setOperands(List<FormulaNode> pOperands) {
    operands = pOperands;
  }
}
