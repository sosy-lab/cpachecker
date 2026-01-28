// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A compound statement in C representing a list of statements wrapped in brackets. Example:
 *
 * <pre>{@code
 * {
 *    statementA;
 *    statementB;
 *    ...
 * }
 * }</pre>
 */
public record CCompoundStatement(ImmutableList<CExportStatement> statements)
    implements CExportStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(System.lineSeparator());
    joiner.add("{");
    for (CExportStatement statement : statements) {
      joiner.add(statement.toASTString(pAAstNodeRepresentation));
    }
    joiner.add("}");
    return joiner.toString();
  }
}
