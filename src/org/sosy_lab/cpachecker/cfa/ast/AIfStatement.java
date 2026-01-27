// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.collect.ImmutableList;
import java.io.Serial;

public abstract class AIfStatement extends AbstractStatement {

  @Serial private static final long serialVersionUID = 4154628454325446837L;

  private final AExpression condition;

  private final ImmutableList<AStatement> ifStatements;

  private final ImmutableList<AStatement> elseStatements;

  /**
   * Creates a new {@code if}-statement. Example output:
   *
   * <pre>{@code
   * if (condition) {
   *    thenStatement
   *    ...
   * } else {
   *    elseStatement
   *    ...
   * }}
   *
   * </pre>
   *
   * @param pFileLocation the location in the source code.
   * @param pCondition the condition expression of the if-statement.
   * @param pIfStatements the list of statements in the 'then' branch. Can be empty.
   * @param pElseStatements the list of statements in the 'else' branch. Can be empty.
   */
  protected AIfStatement(
      FileLocation pFileLocation,
      AExpression pCondition,
      ImmutableList<AStatement> pIfStatements,
      ImmutableList<AStatement> pElseStatements) {

    super(pFileLocation);
    condition = pCondition;
    ifStatements = pIfStatements;
    elseStatements = pElseStatements;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder ifStatement = new StringBuilder();

    ifStatement
        .append("if (")
        .append(condition.toASTString(pAAstNodeRepresentation))
        .append(") {\n");

    for (AStatement statement : ifStatements) {
      ifStatement.append(statement.toASTString(pAAstNodeRepresentation)).append("\n");
    }
    ifStatement.append("}");

    // append the else { ... } branch only if there are any else statements
    if (!elseStatements.isEmpty()) {
      ifStatement.append(" else {\n");
      for (AStatement stmt : elseStatements) {
        ifStatement.append(stmt.toASTString(pAAstNodeRepresentation)).append("\n");
      }
      ifStatement.append("}");
    }

    return ifStatement.toString();
  }
}
