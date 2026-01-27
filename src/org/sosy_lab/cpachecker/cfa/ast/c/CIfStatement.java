// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AIfStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class CIfStatement extends AIfStatement implements CStatement {

  @Serial private static final long serialVersionUID = 2742039470799513955L;

  /**
   * Creates a new {@code if} statement with an optional {@code else} branch. Example output:
   *
   * <pre>{@code
   * if (condition) {
   *    thenStatement;
   *    ...
   * } else {
   *    elseStatement;
   *    ...
   * }
   * }</pre>
   *
   * @param pFileLocation the location in the source code.
   * @param pCondition the condition expression of the if-statement.
   * @param pIfStatements the list of statements in the 'then' branch. Can be empty.
   * @param pElseStatements the list of statements in the 'else' branch. Can be empty.
   */
  CIfStatement(
      FileLocation pFileLocation,
      CExpression pCondition,
      ImmutableList<CStatement> pIfStatements,
      ImmutableList<CStatement> pElseStatements) {

    super(pFileLocation, pCondition, pIfStatements, pElseStatements);
  }

  @Override
  public CExpression getCondition() {
    return (CExpression) super.getCondition();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<CStatement> getIfStatements() {
    return (ImmutableList<CStatement>) super.getIfStatements();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<CStatement> getElseStatements() {
    return (ImmutableList<CStatement>) super.getElseStatements();
  }

  @Override
  public <R, X extends Exception> R accept(CStatementVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder ifStatement = new StringBuilder();

    ifStatement
        .append("if (")
        .append(getCondition().toASTString(pAAstNodeRepresentation))
        .append(") {\n");

    for (AStatement statement : getIfStatements()) {
      ifStatement.append(statement.toASTString(pAAstNodeRepresentation)).append("\n");
    }
    ifStatement.append("}");

    // append the else { ... } branch only if there are any else statements
    if (!getElseStatements().isEmpty()) {
      ifStatement.append(" else {\n");
      for (AStatement stmt : getElseStatements()) {
        ifStatement.append(stmt.toASTString(pAAstNodeRepresentation)).append("\n");
      }
      ifStatement.append("}");
    }

    return ifStatement.toString();
  }
}
