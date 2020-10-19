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

public class NotNode implements FormulaNode {

  private FormulaNode node;

  public NotNode (FormulaNode pNode) {
    node = pNode;
  }

  public NotNode () {}

  public void setNode(FormulaNode pNode) {
    node = pNode;
  }

  @Override
  public String toString() {
    if (node instanceof OrNode || node instanceof ExpressionNode) {
      return "¬" + node;
    }
    return "¬(" + node + ")";
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof NotNode)) {
      return false;
    }
    NotNode notNode = (NotNode) pO;
    return Objects.equals(node, notNode.node);
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, NotNode.class);
  }

  @Override
  public List<FormulaNode> getSuccessors() {
    return ImmutableList.of(node);
  }

  @Override
  public FormulaNodeType getType() {
    return FormulaNodeType.NotNode;
  }
}
