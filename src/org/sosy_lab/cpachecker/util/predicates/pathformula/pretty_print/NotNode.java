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
