// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

// TODO maybe this should be more restricted to And, Or instead of ExpressionTree
public record CExpressionTree(ExpressionTree<CExpression> expressionTree)
    implements CAstExpression {

  // TODO this could also be placed in CAstExpression? the sequentialization only requires the
  //  ExpressionTree to be negated, nothing else
  public String toNegatedASTString() {
    return "!(" + toASTString() + ")";
  }

  @Override
  public String toASTString() {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return expressionTree.toString();
  }
}
