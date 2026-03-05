// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.StringJoiner;
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

  private final CCompoundStatement ifStatement;

  private final Optional<CCompoundStatement> elseStatement;

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
   * @param pIfStatement the non-empty {@link CCompoundStatement} in the {@code if} branch
   */
  public CIfStatement(CExportExpression pCondition, CCompoundStatement pIfStatement) {
    checkArgument(
        !pIfStatement.statements().isEmpty(),
        "pIfStatement needs at least one element, otherwise the if branch is empty.");

    condition = pCondition;
    ifStatement = pIfStatement;
    elseStatement = Optional.empty();
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
   * @param pIfStatement the non-empty {@link CCompoundStatement} in the {@code if} branch
   * @param pElseStatement the non-empty {@link CCompoundStatement} in the {@code else} branch
   */
  public CIfStatement(
      CExportExpression pCondition,
      CCompoundStatement pIfStatement,
      CCompoundStatement pElseStatement) {

    checkArgument(
        !pIfStatement.statements().isEmpty(),
        "pIfStatement needs at least one element, otherwise the if branch is empty.");
    checkArgument(
        !pElseStatement.statements().isEmpty(),
        "pElseStatement needs at least one element, otherwise the else branch is empty.");

    condition = pCondition;
    ifStatement = pIfStatement;
    elseStatement = Optional.of(pElseStatement);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(System.lineSeparator());
    joiner.add("if (" + condition.toASTString(pAAstNodeRepresentation) + ")");
    joiner.add(ifStatement.toASTString(pAAstNodeRepresentation));
    // append the else { ... } branch only if there are any else statements
    if (elseStatement.isPresent()) {
      joiner.add("else");
      joiner.add(elseStatement.orElseThrow().toASTString(pAAstNodeRepresentation));
    }
    return joiner.toString();
  }
}
