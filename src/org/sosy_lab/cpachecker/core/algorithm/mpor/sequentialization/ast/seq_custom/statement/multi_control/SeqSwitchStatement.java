// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSwitchExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
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

  private final SeqSwitchExpression switchExpression;

  private final ImmutableList<CStatement> precedingStatements;

  /**
   * No restriction to literal expressions as keys because e.g. {@code case 1 + 2:} i.e. a {@link
   * CBinaryExpression} is allowed in C.
   */
  private final ImmutableMap<CExpression, ? extends SeqStatement> statements;

  SeqSwitchStatement(
      CLeftHandSide pSwitchExpression,
      ImmutableList<CStatement> pPrecedingStatements,
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements) {

    switchExpression = new SeqSwitchExpression(pSwitchExpression);
    precedingStatements = pPrecedingStatements;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> switchCase = ImmutableList.builder();
    switchCase.addAll(SeqStringUtil.buildLinesOfCodeFromCAstNodes(precedingStatements));
    switchCase.add(SeqStringUtil.appendCurlyBracketLeft(switchExpression.toASTString()));
    switchCase.addAll(buildCases(statements));
    switchCase.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return SeqStringUtil.joinWithNewlines(switchCase.build());
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.SWITCH_CASE;
  }

  private static ImmutableList<String> buildCases(
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rCases = ImmutableList.builder();
    for (var entry : pStatements.entrySet()) {
      SeqStatement statement = entry.getValue();
      String casePrefix = buildCasePrefix(entry.getKey());
      String breakSuffix = buildBreakSuffix(statement);
      rCases.add(casePrefix + statement.toASTString() + breakSuffix);
    }
    return rCases.build();
  }

  private static String buildCasePrefix(CExpression pExpression) {
    return SeqToken.CASE_KEYWORD + SeqSyntax.SPACE + pExpression.toASTString() + SeqSyntax.COLON;
  }

  private static String buildBreakSuffix(SeqStatement pStatement) {
    if (pStatement instanceof SeqSwitchStatement) {
      return SeqSyntax.SPACE + SeqToken.BREAK_KEYWORD + SeqSyntax.SEMICOLON;
    }
    return SeqSyntax.EMPTY_STRING;
  }
}
