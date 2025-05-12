// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
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
    recursivelyBuildTree(statements, statements, tabs, tabs, expression, tree);
    return LineOfCodeUtil.buildStringWithoutTrailingNewline(tree.build());
  }

  // TODO the tree is not balanced atm, see e.g. mix014 unit test. though the impact should be minor
  /**
   * Recursively builds a binary if-else search tree for {@code pStatements} and stores it in {@code
   * pTree}. Note that the labeling does not have to be consecutive from {@code 0}, e.g. {@code 0,
   * 1, 2} or {@code 1, 2, 3} or {@code 0, 2, 3} are all allowed.
   */
  private void recursivelyBuildTree(
      final ImmutableList<? extends SeqStatement> pAllStatements,
      List<? extends SeqStatement> pCurrentStatements,
      final int pInitialDepth,
      int pDepth,
      CLeftHandSide pPc,
      ImmutableList.Builder<LineOfCode> pTree)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    if (size == 1) {
      // single element -> just place statement without any control flow
      pTree.add(LineOfCode.of(pDepth, pCurrentStatements.get(0).toASTString().trim()));

    } else if (size == 2) {
      // only two elements -> create if and else leafs with ==
      int low = pAllStatements.indexOf(pCurrentStatements.get(0));
      pTree.add(buildIfEqualsLeaf(pAllStatements, pInitialDepth, pDepth, low, pPc));
      pTree.add(buildElseLeaf(pCurrentStatements.get(1), pInitialDepth, pDepth));

    } else {
      // more than two elements -> create if and else subtrees with <
      int mid = size / 2;
      SeqStatement midStatement = pCurrentStatements.get(mid);
      // if statement is a clause, use its label number for the < check
      int midIndex = getLabelNumberOrIndex(midStatement, mid);

      pTree.add(buildIfSmallerSubtree(pDepth, midIndex, pPc));
      recursivelyBuildTree(
          pAllStatements,
          pCurrentStatements.subList(0, mid + 1),
          pInitialDepth,
          pDepth + 1,
          pPc,
          pTree);
      pTree.add(buildElseSubtree(pDepth, pCurrentStatements.get(0)));
      recursivelyBuildTree(
          pAllStatements,
          pCurrentStatements.subList(mid + 1, size),
          pInitialDepth,
          pDepth + 1,
          pPc,
          pTree);
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

    SeqStatement lowStatement = pStatements.get(pLow);
    // if statement is a clause, use its label number as the equals check
    int low = getLabelNumberOrIndex(lowStatement, pLow);
    SeqSingleControlFlowStatement ifLeaf =
        new SeqSingleControlFlowStatement(
            binaryExpressionBuilder.buildBinaryExpression(
                pPc,
                SeqExpressionBuilder.buildIntegerLiteralExpression(low),
                BinaryOperator.EQUALS),
            SeqControlFlowStatementType.IF);
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

  // Helpers =======================================================================================

  /**
   * Extracts the label number of {@code pStatement}, or returns {@code pIndex} if not applicable.
   */
  private static int getLabelNumberOrIndex(SeqStatement pStatement, int pIndex) {
    return pStatement instanceof SeqThreadStatementClause
        ? ((SeqThreadStatementClause) pStatement).getLabelNumber()
        : pIndex;
  }
}
