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

public class AndNode implements FormulaNode {

  private FormulaNode left;
  private FormulaNode right;

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
  public String toString() {
    return left + " ∧ " + right;
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof AndNode)) {
      return false;
    }
    AndNode andNode = (AndNode) pO;
    return Objects.equals(left, andNode.left) &&
        Objects.equals(right, andNode.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right, AndNode.class);
  }

  @Override
  public List<FormulaNode> getSuccessors() {
    return ImmutableList.of(left,right);
  }

  @Override
  public FormulaNodeType getType() {
    return FormulaNodeType.AndNode;
  }
}
