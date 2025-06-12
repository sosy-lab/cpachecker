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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIfElseChainStatement implements SeqMultiControlFlowStatement {

  private final CLeftHandSide expression;

  private final int startNumber;

  private final Optional<CFunctionCallStatement> assumption;

  private final ImmutableList<? extends SeqStatement> statements;

  private final int tabs;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqIfElseChainStatement(
      CLeftHandSide pExpression,
      int pStartNumber,
      Optional<CFunctionCallStatement> pAssumption,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    expression = pExpression;
    startNumber = pStartNumber;
    assumption = pAssumption;
    statements = pStatements;
    tabs = pTabs;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList<LineOfCode> ifElseChain =
        buildIfElseChain(
            expression, startNumber, assumption, statements, tabs, binaryExpressionBuilder);
    return LineOfCodeUtil.buildString(ifElseChain);
  }

  private static ImmutableList<LineOfCode> buildIfElseChain(
      CLeftHandSide pExpression,
      int pStartNumber,
      Optional<CFunctionCallStatement> pAssumption,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> ifElseChain = ImmutableList.builder();
    if (pAssumption.isPresent()) {
      ifElseChain.add(LineOfCode.of(pTabs, pAssumption.orElseThrow().toASTString()));
    }
    int currentIndex = pStartNumber;
    for (SeqStatement statement : pStatements) {
      boolean isFirst = currentIndex == pStartNumber;

      // first statement: use "if", otherwise "else if"
      SeqControlFlowStatementType controlStatementType =
          isFirst ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
      CBinaryExpression expressionEquals =
          pBinaryExpressionBuilder.buildBinaryExpression(
              pExpression,
              SeqExpressionBuilder.buildIntegerLiteralExpression(currentIndex),
              BinaryOperator.EQUALS);
      SeqSingleControlFlowStatement controlStatement =
          new SeqSingleControlFlowStatement(expressionEquals, controlStatementType);
      String controlStatementString = controlStatement.toASTString();
      ifElseChain.add(
          LineOfCode.of(
              pTabs,
              isFirst
                  ? SeqStringUtil.appendOpeningCurly(controlStatementString)
                  : SeqStringUtil.wrapInCurlyOutwards(controlStatementString)));

      ifElseChain.add(LineOfCode.of(pTabs + 1, statement.toASTString()));
      currentIndex++;
    }
    ifElseChain.add(LineOfCode.of(pTabs, SeqSyntax.CURLY_BRACKET_RIGHT));
    return ifElseChain.build();
  }

  @Override
  public MultiControlEncoding getEncoding() {
    return MultiControlEncoding.IF_ELSE_CHAIN;
  }
}
