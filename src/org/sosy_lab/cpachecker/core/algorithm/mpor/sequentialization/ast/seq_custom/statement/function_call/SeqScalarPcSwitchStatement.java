// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.function_call;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqScalarPcSwitchStatement implements SeqStatement {

  private final MPOROptions options;

  private final SeqSingleControlFlowStatement switchExpression;

  private final ImmutableList<SeqScalarPcAssumeStatement> clauses;

  private final int tabs;

  public SeqScalarPcSwitchStatement(
      MPOROptions pOptions,
      CExpression pExpression,
      ImmutableList<SeqScalarPcAssumeStatement> pClauses,
      int pTabs) {

    options = pOptions;
    switchExpression =
        new SeqSingleControlFlowStatement(pExpression, SeqControlFlowStatementType.SWITCH);
    clauses = pClauses;
    tabs = pTabs;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder casesString = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (int i = 0; i < clauses.size(); i++) {
      String prefix = SeqToken._case + SeqSyntax.SPACE + i + SeqSyntax.COLON + SeqSyntax.SPACE;
      // tests showed that using break is more efficient than continue, despite the loop
      String breakSuffix = SeqSyntax.SPACE + SeqToken._break + SeqSyntax.SEMICOLON;
      casesString
          .append(
              SeqStringUtil.prependTabsWithoutNewline(
                  tabs + 1, prefix + clauses.get(i).toASTString() + breakSuffix))
          .append(SeqSyntax.NEWLINE);
    }
    String defaultCaseClause =
        SeqToken._default
            + SeqSyntax.COLON
            + SeqSyntax.SPACE
            + Sequentialization.outputReachErrorDummy;
    return SeqStringUtil.buildTab(tabs)
        + SeqStringUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + (options.sequentializationErrors
            ? SeqStringUtil.prependTabsWithNewline(tabs + 1, defaultCaseClause)
            : SeqSyntax.EMPTY_STRING)
        + SeqStringUtil.buildTab(tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
