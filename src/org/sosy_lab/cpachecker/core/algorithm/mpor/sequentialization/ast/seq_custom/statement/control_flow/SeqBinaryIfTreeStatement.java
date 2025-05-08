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
    recursivelyBuildTree(statements, tabs, tabs, 0, statements.size() - 1, expression, tree);
    return LineOfCodeUtil.buildStringWithoutTrailingNewline(tree.build());
  }

  /**
   * Recursively builds a binary if-else search tree for {@code pStatements} and stores it in {@code
   * pTree}. Note that the labeling must be consecutive from {@code 0}, e.g. {@code 0, 1, 2} instead
   * of {@code 1, 2, 3} or {@code 0, 2, 3}.
   */
  private void recursivelyBuildTree(
      final ImmutableList<? extends SeqStatement> pStatements,
      final int pInitialDepth,
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

    if (pHigh - pLow == 1) {
      // only two elements -> create if and else leafs with ==
      pTree.add(buildIfEqualsLeaf(pStatements, pInitialDepth, pDepth, pLow, pPc));
      pTree.add(buildElseLeaf(pStatements.get(pHigh), pInitialDepth, pDepth));

    } else {
      // more than two elements -> create if and else subtrees with <
      int mid = (pLow + pHigh) / 2;
      pTree.add(buildIfSmallerSubtree(pDepth, mid, pPc));
      recursivelyBuildTree(pStatements, pInitialDepth, pDepth + 1, pLow, mid, pPc, pTree);
      pTree.add(buildElseSubtree(pDepth, pStatements.get(pLow)));
      recursivelyBuildTree(pStatements, pInitialDepth, pDepth + 1, mid + 1, pHigh, pPc, pTree);
      pTree.add(LineOfCode.of(pDepth, SeqSyntax.CURLY_BRACKET_RIGHT));
    }
  }

  private LineOfCode buildIfSmallerSubtree(int pDepth, int pMid, CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    SeqSingleControlFlowStatement ifSubtree =
        new SeqSingleControlFlowStatement(
            binaryExpressionBuilder.buildBinaryExpression(
                pPc,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pMid + 1),
                BinaryOperator.LESS_THAN),
            SeqControlFlowStatementType.IF);
    return LineOfCode.of(
        pDepth, ifSubtree.toASTString() + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT);
  }

  private LineOfCode buildElseSubtree(int pDepth, SeqStatement pLowStatement)
      throws UnrecognizedCodeException {

    SeqSingleControlFlowStatement elseSubtree = new SeqSingleControlFlowStatement();
    if (pLowStatement instanceof SeqBinaryIfTreeStatement) {
      // add additional newline prefix, if else subtree is binary tree itself
      return LineOfCode.withNewlinePrefix(
          pDepth, SeqStringUtil.wrapInCurlyOutwards(elseSubtree.toASTString()));
    } else {
      return LineOfCode.of(pDepth, SeqStringUtil.wrapInCurlyOutwards(elseSubtree.toASTString()));
    }
  }

  private LineOfCode buildIfEqualsLeaf(
      ImmutableList<? extends SeqStatement> pStatements,
      int pInitialDepth,
      int pDepth,
      int pLow,
      CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    SeqSingleControlFlowStatement ifLeaf =
        new SeqSingleControlFlowStatement(
            binaryExpressionBuilder.buildBinaryExpression(
                pPc,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pLow),
                BinaryOperator.EQUALS),
            SeqControlFlowStatementType.IF);
    SeqStatement lowStatement = pStatements.get(pLow);
    return buildLeaf(ifLeaf, lowStatement, pInitialDepth, pDepth);
  }

  private LineOfCode buildElseLeaf(SeqStatement pHighStatement, int pInitialDepth, int pDepth)
      throws UnrecognizedCodeException {

    SeqSingleControlFlowStatement elseLeaf = new SeqSingleControlFlowStatement();
    return buildLeaf(elseLeaf, pHighStatement, pInitialDepth, pDepth);
  }

  private LineOfCode buildLeaf(
      SeqSingleControlFlowStatement pControlFlowStatement,
      SeqStatement pStatement,
      int pInitialDepth,
      int pDepth)
      throws UnrecognizedCodeException {

    String prefix = pControlFlowStatement.toASTString() + SeqSyntax.SPACE;
    if (pStatement instanceof SeqBinaryIfTreeStatement) {
      // no newline if the statement itself is a binary tree for formatting
      String code =
          SeqStringUtil.wrapInCurlyInwardsWithNewlines(pStatement.toASTString(), 0, pDepth);
      if (pControlFlowStatement.type.equals(SeqControlFlowStatementType.ELSE)) {
        // no additional tabs, when else leaf is binary tree so that "} else {" is compact
        return LineOfCode.withoutNewlineSuffix(0, SeqSyntax.SPACE + prefix + code);
      }
      return LineOfCode.withoutNewlineSuffix(pDepth, prefix + code);
    } else {
      String code = SeqStringUtil.wrapInCurlyInwards(pStatement.toASTString());
      if (pControlFlowStatement.type.equals(SeqControlFlowStatementType.ELSE)) {
        if (pInitialDepth == pDepth) {
          // align if-else leafs
          return LineOfCode.of(pDepth - 1, prefix + code);
        }
      }
      return LineOfCode.of(pDepth, prefix + code);
    }
  }
}
