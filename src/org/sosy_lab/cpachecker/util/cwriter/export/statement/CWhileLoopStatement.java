// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

/**
 * Used to export {@code while} loop statements. Example:
 *
 * <pre>{@code
 * while (i < N) {
 *    statement;
 *    ...
 * }
 * }</pre>
 *
 * <p>Note that statements such as the initialization of {@code i} and {@code N} and the increment
 * of {@code i} are not specified in this class and must be handled by the creator of this class.
 *
 * @param condition The condition for the control flow to enter the loop, e.g., {@code i < N} in
 *     {@code while (i < N)}.
 * @param body The compound statement inside the loop. Can be empty, e.g., to simulate an infinite
 *     loop that does nothing.
 */
public record CWhileLoopStatement(CExportExpression condition, CCompoundStatement body)
    implements CExportStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(System.lineSeparator());
    joiner.add("while (" + condition().toASTString(pAAstNodeRepresentation) + ")");
    joiner.add(body().toASTString(pAAstNodeRepresentation));
    return joiner.toString();
  }
}
