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

public class LiteralNode implements FormulaNode {

  private final String literal;

  public static final LiteralNode OPEN_BRACKET = new LiteralNode("(");
  public static final LiteralNode CLOSE_BRACKET = new LiteralNode(")");
  public static final LiteralNode TRUE = new LiteralNode("TRUE");
  public static final LiteralNode FALSE = new LiteralNode("FALSE");
  public static final LiteralNode EMPTY = new LiteralNode("()");

  public LiteralNode(String pLiteral) {
    literal = pLiteral;
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof LiteralNode)) {
      return false;
    }
    LiteralNode that = (LiteralNode) pO;
    return Objects.equals(literal, that.literal);
  }

  @Override
  public String toString() {
    return literal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(literal);
  }

  @Override
  public List<FormulaNode> getSuccessors() {
    return ImmutableList.of();
  }

  @Override
  public FormulaNodeType getType() {
    return FormulaNodeType.LiteralNode;
  }
}
