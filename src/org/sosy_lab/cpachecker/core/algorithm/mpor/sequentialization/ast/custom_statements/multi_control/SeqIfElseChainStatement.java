// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;

public record SeqIfElseChainStatement(
    ImmutableList<CExportStatement> precedingStatements,
    ImmutableMap<CExportExpression, ? extends CExportStatement> statements)
    implements SeqMultiControlStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner ifElseChain = new StringJoiner(SeqSyntax.NEWLINE);

    // first add preceding statements
    for (CExportStatement precedingStatement : precedingStatements) {
      ifElseChain.add(precedingStatement.toASTString(pAAstNodeRepresentation));
    }

    // then add all statements via if ... else { if ... } from the bottom up
    ImmutableList<Entry<CExportExpression, ? extends CExportStatement>> statementList =
        ImmutableList.copyOf(statements.entrySet());
    CIfStatement currentBranch = null;
    for (int i = statementList.size() - 1; i >= 0; i--) {
      Entry<CExportExpression, ? extends CExportStatement> currentStatement = statementList.get(i);
      // on last statement: no else branch
      if (i == statementList.size() - 1) {
        currentBranch =
            new CIfStatement(
                currentStatement.getKey(), new CCompoundStatement(currentStatement.getValue()));
      } else {
        currentBranch =
            new CIfStatement(
                currentStatement.getKey(),
                new CCompoundStatement(currentStatement.getValue()),
                new CCompoundStatement(Objects.requireNonNull(currentBranch)));
      }
    }
    ifElseChain.add(Objects.requireNonNull(currentBranch).toASTString(pAAstNodeRepresentation));
    return ifElseChain.toString();
  }
}
