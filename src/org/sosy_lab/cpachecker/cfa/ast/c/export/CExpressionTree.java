// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

// TODO maybe this should be more restricted to And, Or instead of ExpressionTree
public record CExpressionTree(ExpressionTree<CExportExpression> expressionTree)
    implements CExportExpression {

  // TODO this could be moved into CAstExpression, but at the moment we only need it here
  public CNegatedExpression negate() {
    return new CNegatedExpression(this);
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
