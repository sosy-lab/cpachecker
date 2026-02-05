// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqASTNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

public sealed interface SeqExportStatement extends SeqASTNode
    permits SeqBlockLabelStatement,
        SeqInjectedStatement,
        SeqThreadStatementBlock,
        SeqThreadStatementClause {

  ImmutableList<CExportStatement> toCExportStatements();

  @Override
  default String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(System.lineSeparator());
    for (CExportStatement statement : toCExportStatements()) {
      joiner.add(statement.toASTString(pAAstNodeRepresentation));
    }
    return joiner.toString();
  }
}
