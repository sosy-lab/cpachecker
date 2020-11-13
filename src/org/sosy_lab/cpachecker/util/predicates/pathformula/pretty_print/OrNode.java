// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

public class OrNode implements FormulaNode {

  private FormulaNode left;
  private FormulaNode right;

  public OrNode() {
  }

  public void setLeft(FormulaNode pLeft) {
    left = pLeft;
  }

  public void setRight(FormulaNode pRight) {
    right = pRight;
  }

  public FormulaNode getLeft() {
    return left;
  }

  public FormulaNode getRight() {
    return right;
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof OrNode)) {
      return false;
    }
    OrNode orNode = (OrNode) pO;
    return Objects.equals(left, orNode.left) &&
        Objects.equals(right, orNode.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right, OrNode.class);
  }

  @Override
  public List<FormulaNode> getSuccessors() {
    return ImmutableList.of(left,right);
  }

  @Override
  public FormulaNodeType getType() {
    return FormulaNodeType.OrNode;
  }

  @Override
  public String toString() {
    return "(" + left + " v " + right + ")";
  }
}
