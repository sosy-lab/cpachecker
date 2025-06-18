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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
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

  private final CLeftHandSide expression;

  // TODO instead of a startNumber, we should have a list of expressions
  //  because conflicts do not have consecutive numbers
  private final int startNumber;

  private final Optional<CFunctionCallStatement> assumption;

  private final Optional<CExpressionAssignmentStatement> lastThreadUpdate;

  private final ImmutableList<? extends SeqStatement> statements;

  private final int tabs;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  SeqIfElseChainStatement(
      CLeftHandSide pExpression,
      int pStartNumber,
      Optional<CFunctionCallStatement> pAssumption,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    expression = pExpression;
    startNumber = pStartNumber;
    assumption = pAssumption;
    lastThreadUpdate = pLastThreadUpdate;
    statements = pStatements;
    tabs = pTabs;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    if (assumption.isPresent()) {
      ifElseChain.add(LineOfCode.of(tabs, assumption.orElseThrow().toASTString()));
    }
    ifElseChain.addAll(
        buildIfElseChain(expression, startNumber, statements, tabs, binaryExpressionBuilder));
    // TODO the problem here is that with continue; this becomes unreachable -> fix
    if (lastThreadUpdate.isPresent()) {
      ifElseChain.add(LineOfCode.of(tabs, lastThreadUpdate.orElseThrow().toASTString()));
    }
    return LineOfCodeUtil.buildString(ifElseChain.build());
  }

  private static ImmutableList<LineOfCode> buildIfElseChain(
      CLeftHandSide pExpression,
      int pStartNumber,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    int currentIndex = pStartNumber;
    for (SeqStatement statement : pStatements) {
      boolean isFirst = currentIndex == pStartNumber;
      CBinaryExpression expressionEquals =
          pBinaryExpressionBuilder.buildBinaryExpression(
              pExpression,
              SeqExpressionBuilder.buildIntegerLiteralExpression(currentIndex),
              BinaryOperator.EQUALS);

      // first statement: use "if", otherwise "else if"
      SeqSingleControlExpression controlExpression =
          isFirst
              ? new SeqIfExpression(expressionEquals)
              : new SeqElseIfExpression(expressionEquals);
      String controlStatementString = controlExpression.toASTString();
      ifElseChain.add(
          LineOfCode.of(
              pTabs,
              isFirst
                  ? SeqStringUtil.appendCurlyBracketRight(controlStatementString)
                  : SeqStringUtil.wrapInCurlyBracketsOutwards(controlStatementString)));

      ifElseChain.add(LineOfCode.of(pTabs + 1, statement.toASTString()));
      currentIndex++;
    }
    ifElseChain.add(LineOfCode.of(pTabs, SeqSyntax.CURLY_BRACKET_RIGHT));
    return ifElseChain.build();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.IF_ELSE_CHAIN;
  }
}
