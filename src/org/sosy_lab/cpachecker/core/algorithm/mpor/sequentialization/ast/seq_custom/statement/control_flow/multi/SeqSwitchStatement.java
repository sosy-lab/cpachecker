// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
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

  private final MPOROptions options;

  private final SeqSingleControlFlowStatement switchExpression;

  private final Optional<CFunctionCallStatement> assumptions;

  private final ImmutableList<? extends SeqStatement> statements;

  private final int tabs;

  public SeqSwitchStatement(
      MPOROptions pOptions,
      CExpression pExpression,
      Optional<CFunctionCallStatement> pAssumption,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs) {

    options = pOptions;
    switchExpression =
        new SeqSingleControlFlowStatement(pExpression, SeqControlFlowStatementType.SWITCH);
    assumptions = pAssumption;
    statements = pStatements;
    tabs = pTabs;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String assumptionsString = buildAssumptionsString(assumptions, tabs);
    String casesString = buildCasesString(statements, tabs);
    return assumptionsString
        + SeqStringUtil.buildTab(tabs)
        + SeqStringUtil.appendOpeningCurly(switchExpression.toASTString())
        + SeqSyntax.NEWLINE
        + casesString
        + (options.sequentializationErrors
            ? SeqStringUtil.prependTabsWithNewline(
                tabs + 1, Sequentialization.defaultCaseClauseError)
            : SeqSyntax.EMPTY_STRING)
        + SeqStringUtil.buildTab(tabs)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public MultiControlEncoding getEncoding() {
    return MultiControlEncoding.SWITCH_CASE;
  }

  private static String buildAssumptionsString(
      Optional<CFunctionCallStatement> pAssumption, int pTabs) {

    if (pAssumption.isEmpty()) {
      return SeqSyntax.EMPTY_STRING;
    }
    return LineOfCode.of(pTabs, pAssumption.orElseThrow().toASTString()).toString();
  }

  private static String buildCasesString(
      ImmutableList<? extends SeqStatement> pStatements, int pTabs)
      throws UnrecognizedCodeException {

    StringBuilder casesString = new StringBuilder();
    for (int i = 0; i < pStatements.size(); i++) {
      SeqStatement statement = pStatements.get(i);
      String casePrefix = buildCasePrefix(statement, i);
      String breakSuffix = buildBreakSuffix(statement);
      casesString.append(buildSingleCase(pTabs, casePrefix, statement, breakSuffix));
    }
    return casesString.toString();
  }

  private static String buildSingleCase(
      int pTabs, String pPrefix, SeqStatement pStatement, String pSuffix)
      throws UnrecognizedCodeException {

    return SeqStringUtil.prependTabsWithoutNewline(
        pTabs + 1, pPrefix + pStatement.toASTString() + pSuffix + SeqSyntax.NEWLINE);
  }

  private static String buildCasePrefix(SeqStatement pStatement, int pIndex) {
    if (pStatement instanceof SeqThreadStatementClause clause) {
      // if case statement is clause, use label number
      return buildCaseWithLabelNumber(clause, clause.labelNumber);
    } else {
      // otherwise enumerate from 0 to caseNum - 1
      return buildCaseWithLabelNumber(pStatement, pIndex);
    }
  }

  private static String buildCaseWithLabelNumber(SeqStatement pStatement, int pLabelNumber) {
    return SeqToken._case
        + SeqSyntax.SPACE
        + pLabelNumber
        + SeqSyntax.COLON
        + (pStatement instanceof SeqSwitchStatement
            ? SeqSyntax.NEWLINE
            : SeqStringUtil.buildSpaceAlign(pLabelNumber));
  }

  private static String buildBreakSuffix(SeqStatement pStatement) {
    if (pStatement instanceof SeqSwitchStatement) {
      return SeqSyntax.SPACE + SeqToken._break + SeqSyntax.SEMICOLON;
    }
    return SeqSyntax.EMPTY_STRING;
  }
}
