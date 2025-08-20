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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqElseIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIfElseChainStatement implements SeqMultiControlStatement {

  private final ImmutableList<CStatement> precedingStatements;

  private final ImmutableMap<CExpression, ? extends SeqStatement> statements;

  SeqIfElseChainStatement(
      ImmutableList<CStatement> pPrecedingStatements,
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements) {

    precedingStatements = pPrecedingStatements;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    ifElseChain.addAll(LineOfCodeUtil.buildLinesOfCodeFromCAstNodes(precedingStatements));
    ifElseChain.addAll(buildIfElseChain(statements));
    return LineOfCodeUtil.buildString(ifElseChain.build());
  }

  private static ImmutableList<LineOfCode> buildIfElseChain(
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    boolean isFirst = true;
    for (var statement : pStatements.entrySet()) {
      // first statement: use "if", otherwise "else if"
      SeqSingleControlExpression controlExpression =
          isFirst
              ? new SeqIfExpression(statement.getKey())
              : new SeqElseIfExpression(statement.getKey());
      String controlStatementString = controlExpression.toASTString();
      ifElseChain.add(
          LineOfCode.of(
              isFirst
                  ? SeqStringUtil.appendCurlyBracketRight(controlStatementString)
                  : SeqStringUtil.wrapInCurlyBracketsOutwards(controlStatementString)));
      ifElseChain.add(LineOfCode.of(statement.getValue().toASTString()));
      isFirst = false;
    }
    ifElseChain.add(LineOfCode.of(SeqSyntax.CURLY_BRACKET_RIGHT));
    return ifElseChain.build();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.IF_ELSE_CHAIN;
  }
}
