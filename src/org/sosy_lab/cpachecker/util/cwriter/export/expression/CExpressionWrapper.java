// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.expression;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * A wrapper class for {@link CExpression} so that it can be treated like a {@link
 * CExportExpression}.
 */
public record CExpressionWrapper(CExpression expression) implements CExportExpression {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return expression.toASTString(pAAstNodeRepresentation);
  }
}
