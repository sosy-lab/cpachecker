// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a binary search tree with {@code if-else} branches. */
public class SeqBinaryIfTreeStatement implements SeqMultiControlFlowStatement {

  private final CLeftHandSide expression;

  private final ImmutableList<? extends SeqStatement> statements;

  private final int tabs;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqBinaryIfTreeStatement(
      CLeftHandSide pExpression,
      ImmutableList<? extends SeqStatement> pStatements,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    expression = pExpression;
    statements = pStatements;
    tabs = pTabs;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> tree = ImmutableList.builder();
    recursivelyBuildTree(statements, tabs, 0, statements.size() - 1, expression, tree);
    return LineOfCodeUtil.buildString(tree.build());
  }

  private void recursivelyBuildTree(
      final ImmutableList<? extends SeqStatement> pStatements,
      int pDepth,
      int pLow,
      int pHigh,
      CLeftHandSide pPc,
      ImmutableList.Builder<LineOfCode> pTree)
      throws UnrecognizedCodeException {

    if (pLow == pHigh) {
      // single element -> just place statement without any control flow
      pTree.add(LineOfCode.of(pDepth, pStatements.get(pLow).toASTString().trim()));
      return;
    }

    SeqSingleControlFlowStatement elseStatement = new SeqSingleControlFlowStatement();
    if (pHigh - pLow == 1) {
      // only two elements -> compa re directly with ==
      SeqSingleControlFlowStatement ifStatement =
          new SeqSingleControlFlowStatement(
              binaryExpressionBuilder.buildBinaryExpression(
                  pPc,
                  SeqExpressionBuilder.buildIntegerLiteralExpression(pLow),
                  BinaryOperator.EQUALS),
              SeqControlFlowStatementType.IF);
      pTree.add(
          LineOfCode.of(
              pDepth,
              ifStatement.toASTString()
                  + SeqSyntax.SPACE
                  + SeqStringUtil.wrapInCurlyInwardsWithNewlines(
                      pStatements.get(pLow).toASTString(), pDepth)));
      pTree.add(
          LineOfCode.of(
              pDepth,
              elseStatement.toASTString()
                  + SeqSyntax.SPACE
                  + SeqStringUtil.wrapInCurlyInwards(pStatements.get(pHigh).toASTString())));

    } else {
      // more than two elements -> create new subtree with <
      int mid = (pLow + pHigh) / 2;
      SeqSingleControlFlowStatement ifStatement =
          new SeqSingleControlFlowStatement(
              binaryExpressionBuilder.buildBinaryExpression(
                  pPc,
                  SeqExpressionBuilder.buildIntegerLiteralExpression(mid + 1),
                  BinaryOperator.LESS_THAN),
              SeqControlFlowStatementType.IF);
      pTree.add(
          LineOfCode.of(
              pDepth, ifStatement.toASTString() + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT));
      recursivelyBuildTree(pStatements, pDepth + 1, pLow, mid, pPc, pTree);
      pTree.add(
          LineOfCode.of(pDepth, SeqStringUtil.wrapInCurlyOutwards(elseStatement.toASTString())));
      recursivelyBuildTree(pStatements, pDepth + 1, mid + 1, pHigh, pPc, pTree);
      pTree.add(LineOfCode.of(pDepth, SeqSyntax.CURLY_BRACKET_RIGHT));
    }
  }
}
