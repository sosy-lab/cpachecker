// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

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
public final class CIfStatement implements CExportStatement {

  private final CExportExpression condition;

  private final ImmutableList<CExportStatement> ifStatements;

  private final ImmutableList<CExportStatement> elseStatements;

  /**
   * Use this constructor to create an {@code if} statement without any {@code else} branch:
   *
   * <pre>{@code
   * if (condition) {
   *    ifStatement;
   *    ...
   * }
   * }</pre>
   *
   * @param pCondition the {@code if} condition
   * @param pIfStatements the non-empty list of statements in the {@code if} branch
   */
  public CIfStatement(CExportExpression pCondition, ImmutableList<CExportStatement> pIfStatements) {
    checkArgument(
        !pIfStatements.isEmpty(),
        "pIfStatements needs at least one element, otherwise the if branch is empty.");

    condition = pCondition;
    ifStatements = pIfStatements;
    elseStatements = ImmutableList.of();
  }

  /**
   * Use this constructor to create an {@code if} statement with an {@code else} branch:
   *
   * <pre>{@code
   * if (condition) {
   *    ifStatement;
   *    ...
   * } else {
   *   elseStatement;
   *   ...
   * }
   * }</pre>
   *
   * @param pCondition the {@code if} condition
   * @param pIfStatements the non-empty list of statements in the {@code if} branch
   * @param pElseStatements the non-empty list of statements in the {@code else} branch
   */
  public CIfStatement(
      CExportExpression pCondition,
      ImmutableList<CExportStatement> pIfStatements,
      ImmutableList<CExportStatement> pElseStatements) {

    checkArgument(
        !pIfStatements.isEmpty(),
        "pIfStatements needs at least one element, otherwise the if branch is empty.");
    checkArgument(
        !pElseStatements.isEmpty(),
        "pElseStatements needs at least one element, otherwise the else branch is empty.");

    condition = pCondition;
    ifStatements = pIfStatements;
    elseStatements = pElseStatements;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringBuilder ifStatement = new StringBuilder();

    ifStatement
        .append("if (")
        .append(condition.toASTString(pAAstNodeRepresentation))
        .append(") {\n");

    for (CExportStatement statement : ifStatements) {
      ifStatement.append(statement.toASTString(pAAstNodeRepresentation)).append("\n");
    }
    ifStatement.append("}");

    // append the else { ... } branch only if there are any else statements
    if (!elseStatements.isEmpty()) {
      ifStatement.append(" else {\n");
      for (CExportStatement stmt : elseStatements) {
        ifStatement.append(stmt.toASTString(pAAstNodeRepresentation)).append("\n");
      }
      ifStatement.append("}");
    }

    return ifStatement.toString();
  }
}
