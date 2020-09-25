/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
