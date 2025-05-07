// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents the entirety of a switch statement.
 *
 * <p>Example: {@code switch(a) { case b: ...; break; case c: ...; break; } }
 *
 * <p>with the default sequentialization error {@code default: reach_error(...); }
 */
public class SeqSwitchStatement implements SeqMultiControlFlowStatement {

  /** A label with {@code case 123:} has length 10. */
  private static final int MAX_LABEL_LENGTH = 6 + SeqStringUtil.MAX_ALIGN;

  private final MPOROptions options;

  private final SeqSingleControlFlowStatement switchExpression;

  private final ImmutableList<? extends SeqStatement> clauses;

  private final int tabs;

  public SeqSwitchStatement(
      MPOROptions pOptions,
      CExpression pExpression,
      ImmutableList<? extends SeqStatement> pClauses,
      int pTabs) {

    options = pOptions;
    switchExpression =
        new SeqSingleControlFlowStatement(pExpression, SeqControlFlowStatementType.SWITCH);
    clauses = pClauses;
    tabs = pTabs;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder casesString = new StringBuilder();
    for (int i = 0; i < clauses.size(); i++) {
      String casePrefix = buildCasePrefix(clauses.get(i), i);
      String breakSuffix = SeqSyntax.SPACE + SeqToken._break + SeqSyntax.SEMICOLON;
      casesString
          .append(
              SeqStringUtil.prependTabsWithoutNewline(
                  tabs + 1, casePrefix + clauses.get(i).toASTString()))
          .append(breakSuffix)
          .append(SeqSyntax.NEWLINE);
    }
    String defaultCaseClause =
        SeqToken._default
            + SeqSyntax.COLON
            + SeqSyntax.SPACE
            + Sequentialization.outputReachErrorDummy;
    return SeqSyntax.NEWLINE
        + SeqStringUtil.buildTab(tabs)
        + SeqStringUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + (options.sequentializationErrors
            ? SeqStringUtil.prependTabsWithNewline(tabs + 1, defaultCaseClause)
            : SeqSyntax.EMPTY_STRING)
        + SeqStringUtil.buildTab(tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  private String buildCasePrefix(SeqStatement pClause, int pCaseNum) {
    if (pClause instanceof SeqThreadStatementClause threadClause) {
      if (threadClause.block.startsInAtomicBlock()) {
        // the start of an atomic block does not have to be directly reachable, no case needed
        return SeqSyntax.SPACE.repeat(MAX_LABEL_LENGTH);
      }
    }
    return SeqToken._case
        + SeqSyntax.SPACE
        + pCaseNum
        + SeqSyntax.COLON
        + SeqStringUtil.buildSpaceAlign(pCaseNum);
  }
}
