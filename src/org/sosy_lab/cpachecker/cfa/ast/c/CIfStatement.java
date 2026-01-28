// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;

/**
 * Represents an {@code if} statement with an optional {@code else} branch. Example output:
 *
 * <pre>{@code
 * if (condition) {
 *    ifStatement;
 *    ...
 * } else {
 *    elseStatement;
 *    ...
 * }
 * }</pre>
 */
public record CIfStatement(
    CExpression condition,
    ImmutableList<CAstStatement> ifStatements,
    ImmutableList<CAstStatement> elseStatements)
    implements CAstStatement {

  @Override
  public String toASTString() {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder ifStatement = new StringBuilder();

    ifStatement
        .append("if (")
        .append(condition.toASTString(pAAstNodeRepresentation))
        .append(") {\n");

    for (CAstStatement statement : ifStatements) {
      ifStatement.append(statement.toASTString(pAAstNodeRepresentation)).append("\n");
    }
    ifStatement.append("}");

    // append the else { ... } branch only if there are any else statements
    if (!elseStatements.isEmpty()) {
      ifStatement.append(" else {\n");
      for (CAstStatement stmt : elseStatements) {
        ifStatement.append(stmt.toASTString(pAAstNodeRepresentation)).append("\n");
      }
      ifStatement.append("}");
    }

    return ifStatement.toString();
  }
}
