// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print;

import java.util.List;

public interface FormulaNode {

  enum FormulaNodeType {
    AndNode, OrNode, LiteralNode, ExpressionNode, NotNode
  }

  List<FormulaNode> getSuccessors();

  FormulaNodeType getType();

}
