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
 * while (1) {
 *    statement;
 *    ...
 * }
 * }</pre>
 */
public final class CWhileLoopStatement extends CLoopStatement {

  public CWhileLoopStatement(CExportExpression pCondition, CCompoundStatement pCompoundStatement) {
    super(pCondition, pCompoundStatement);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(System.lineSeparator());
    joiner.add("while (" + getCondition().toASTString(pAAstNodeRepresentation) + ")");
    joiner.add(getBody().toASTString(pAAstNodeRepresentation));
    return joiner.toString();
  }
}
