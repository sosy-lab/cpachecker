// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.parser;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExpressionNode implements FormulaNode {

  private String operator;
  private List<ExpressionNode> operands;

  public ExpressionNode(String pOperator) {
    operator = pOperator;
    operands = new ArrayList<>();
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

  public List<ExpressionNode> getOperands() {
    return operands;
  }

  @Override
  public boolean equals(Object pO) {
    if (pO == null  || !(pO instanceof ExpressionNode)) {
      return false;
    }
    ExpressionNode that = (ExpressionNode) pO;
    if (operands.size() != that.operands.size()) {
      return false;
    }
    for (int i = 0; i < operands.size(); i++) {
      if (!(operands.get(i).equals(that.operands.get(i)))) {
        return false;
      }
    }
    return Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    int hashCode = 17;
    hashCode = Objects.hash(hashCode, operator);
    for (ExpressionNode operand : operands) {
      hashCode = Objects.hash(hashCode, operand);
    }
    return hashCode;
  }

  @Override
  public String toString() {
    String op = operator;
    if (op.contains("_") && !op.startsWith("_")) {
      op = op.split("_")[0];
    }
    if (operands.size() == 2) {
      return "(" + operands.get(0) + " " + op + " "  + operands.get(1) + ")";
    }
    return "(" + op + " " + operands.stream().map(opt -> opt.toString()).collect(Collectors.joining(" ")) + ")";
  }

  public void setOperands(List<ExpressionNode> pOperands) {
    operands = pOperands;
  }
}
