// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSwitchExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
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
public class SeqSwitchStatement implements SeqMultiControlStatement {

  private final MPOROptions options;

  private final SeqSwitchExpression switchExpression;

  private final Optional<CFunctionCallStatement> assumption;

  private final Optional<CExpressionAssignmentStatement> lastThreadUpdate;

  private final ImmutableList<? extends SeqStatement> statements;

  private final int tabs;

  SeqSwitchStatement(
      MPOROptions pOptions,
      CLeftHandSide pExpression,
      Optional<CFunctionCallStatement> pAssumption,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs) {

    options = pOptions;
    switchExpression = new SeqSwitchExpression(pExpression);
    assumption = pAssumption;
    lastThreadUpdate = pLastThreadUpdate;
    statements = pStatements;
    tabs = pTabs;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> switchCase = ImmutableList.builder();
    if (assumption.isPresent()) {
      switchCase.add(LineOfCode.of(tabs, assumption.orElseThrow().toASTString()));
    }
    switchCase.add(
        LineOfCode.of(
            tabs, SeqStringUtil.appendOpeningCurlyBrackets(switchExpression.toASTString())));
    switchCase.addAll(buildCases(options, statements, tabs));
    switchCase.add(LineOfCode.of(tabs, SeqSyntax.CURLY_BRACKET_RIGHT));
    if (lastThreadUpdate.isPresent()) {
      switchCase.add(LineOfCode.of(tabs, lastThreadUpdate.orElseThrow().toASTString()));
    }
    return LineOfCodeUtil.buildString(switchCase.build());
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.SWITCH_CASE;
  }

  private static ImmutableList<LineOfCode> buildCases(
      MPOROptions pOptions, ImmutableList<? extends SeqStatement> pStatements, int pTabs)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rCases = ImmutableList.builder();
    for (int i = 0; i < pStatements.size(); i++) {
      SeqStatement statement = pStatements.get(i);
      String casePrefix = buildCasePrefix(statement, i);
      String breakSuffix = buildBreakSuffix(statement);
      rCases.add(buildSingleCase(pTabs, casePrefix, statement, breakSuffix));
      if (i == pStatements.size() - 1) {
        if (pOptions.sequentializationErrors) {
          rCases.add(LineOfCode.of(pTabs + 1, Sequentialization.defaultCaseClauseError));
        }
      }
    }
    return rCases.build();
  }

  private static LineOfCode buildSingleCase(
      int pTabs, String pPrefix, SeqStatement pStatement, String pSuffix)
      throws UnrecognizedCodeException {

    return LineOfCode.of(pTabs + 1, pPrefix + pStatement.toASTString() + pSuffix);
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
