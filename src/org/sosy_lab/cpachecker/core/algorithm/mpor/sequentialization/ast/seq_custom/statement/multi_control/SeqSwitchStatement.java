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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSwitchExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
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

  private final ImmutableList<CStatement> precedingStatements;

  private final Optional<CExpressionAssignmentStatement> lastThreadUpdate;

  /**
   * No restriction to literal expressions as keys because e.g. {@code case 1 + 2:} i.e. a {@link
   * CBinaryExpression} is allowed in C.
   */
  private final ImmutableMap<CExpression, ? extends SeqStatement> statements;

  SeqSwitchStatement(
      MPOROptions pOptions,
      CLeftHandSide pSwitchExpression,
      ImmutableList<CStatement> pPrecedingStatements,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements) {

    options = pOptions;
    switchExpression = new SeqSwitchExpression(pSwitchExpression);
    precedingStatements = pPrecedingStatements;
    lastThreadUpdate = pLastThreadUpdate;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> switchCase = ImmutableList.builder();
    switchCase.addAll(LineOfCodeUtil.buildLinesOfCode(precedingStatements));
    switchCase.add(
        LineOfCode.of(SeqStringUtil.appendCurlyBracketRight(switchExpression.toASTString())));
    switchCase.addAll(buildCases(options, statements));
    switchCase.add(LineOfCode.of(SeqSyntax.CURLY_BRACKET_RIGHT));
    if (lastThreadUpdate.isPresent()) {
      switchCase.add(LineOfCode.of(lastThreadUpdate.orElseThrow().toASTString()));
    }
    return LineOfCodeUtil.buildString(switchCase.build());
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.SWITCH_CASE;
  }

  private static ImmutableList<LineOfCode> buildCases(
      MPOROptions pOptions, ImmutableMap<CExpression, ? extends SeqStatement> pStatements)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rCases = ImmutableList.builder();
    int index = 0;
    for (var entry : pStatements.entrySet()) {
      boolean isLast = index == pStatements.size() - 1;
      SeqStatement statement = entry.getValue();
      String casePrefix = buildCasePrefix(entry.getKey());
      String breakSuffix = buildBreakSuffix(statement);
      rCases.add(buildSingleCase(casePrefix, statement, breakSuffix));
      if (isLast && pOptions.sequentializationErrors) {
        rCases.add(LineOfCode.of(Sequentialization.defaultCaseClauseError));
      }
    }
    return rCases.build();
  }

  private static LineOfCode buildSingleCase(String pPrefix, SeqStatement pStatement, String pSuffix)
      throws UnrecognizedCodeException {

    return LineOfCode.of(pPrefix + pStatement.toASTString() + pSuffix);
  }

  private static String buildCasePrefix(CExpression pExpression) {
    return SeqToken._case + SeqSyntax.SPACE + pExpression.toASTString() + SeqSyntax.COLON;
  }

  private static String buildBreakSuffix(SeqStatement pStatement) {
    if (pStatement instanceof SeqSwitchStatement) {
      return SeqSyntax.SPACE + SeqToken._break + SeqSyntax.SEMICOLON;
    }
    return SeqSyntax.EMPTY_STRING;
  }
}
