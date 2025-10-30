// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.BranchType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a binary search tree with {@code if-else} branches. */
public record SeqBinarySearchTreeStatement(
    CLeftHandSide expression,
    ImmutableList<CStatement> precedingStatements,
    ImmutableMap<CExpression, ? extends SeqStatement> statements,
    CBinaryExpressionBuilder binaryExpressionBuilder)
    implements SeqMultiControlStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner tree = new StringJoiner(SeqSyntax.NEWLINE);
    precedingStatements.forEach(statement -> tree.add(statement.toASTString()));
    ImmutableList<Entry<CExpression, ? extends SeqStatement>> statementList =
        ImmutableList.copyOf(statements.entrySet());
    recursivelyBuildTree(statementList, statementList, expression, tree);
    return tree.toString();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.BINARY_SEARCH_TREE;
  }

  /**
   * Recursively builds a binary if-else search tree for {@code pAllStatements} and stores it in
   * {@code pTree}. Note that the labeling does not have to be consecutive from {@code 0}, e.g.
   * {@code 0, 1, 2} or {@code 1, 2, 3} or {@code 0, 2, 3} are all allowed.
   */
  private void recursivelyBuildTree(
      final ImmutableList<Entry<CExpression, ? extends SeqStatement>> pAllStatements,
      List<Entry<CExpression, ? extends SeqStatement>> pCurrentStatements,
      CLeftHandSide pPc,
      StringJoiner pTree)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    if (size == 1) {
      // single element -> just place statement without any control flow
      pTree.add(pCurrentStatements.getFirst().getValue().toASTString());

    } else if (size == 2) {
      // only two elements -> create if and else leafs with ==
      Entry<CExpression, ? extends SeqStatement> ifStatement = pCurrentStatements.getFirst();
      SeqStatement elseStatement = pCurrentStatements.get(1).getValue();
      pTree.add(buildLeaf(ifStatement.getKey(), ifStatement.getValue(), elseStatement));

    } else {
      // more than two elements -> create if and else subtrees with <
      int mid = size / 2;
      Entry<CExpression, ? extends SeqStatement> midEntry = pCurrentStatements.get(mid);
      SeqStatement midStatement = midEntry.getValue();
      // if statement is a clause, use its label number for the < check
      int midIndex = getLabelNumberOrIndex(midStatement, pAllStatements.indexOf(midEntry)) - 1;

      pTree.add(buildIfSmallerSubtree(midIndex, pPc));
      recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(0, mid), pPc, pTree);
      pTree.add(buildElseSubtree());
      recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(mid, size), pPc, pTree);
      pTree.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    }
  }

  private String buildIfSmallerSubtree(int pMid, CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    CBinaryExpression ifExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            pPc,
            SeqExpressionBuilder.buildIntegerLiteralExpression(pMid + 1),
            BinaryOperator.LESS_THAN);
    return BranchType.IF.buildPrefix(ifExpression.toASTString());
  }

  private String buildElseSubtree() {
    return BranchType.ELSE.buildPrefix();
  }

  private String buildLeaf(
      CExpression pIfExpression, SeqStatement pIfBranchStatement, SeqStatement pElseIfStatement)
      throws UnrecognizedCodeException {

    SeqBranchStatement ifElseLeaf =
        new SeqBranchStatement(
            pIfExpression.toASTString(),
            ImmutableList.of(pIfBranchStatement.toASTString()),
            ImmutableList.of(pElseIfStatement.toASTString()));
    return ifElseLeaf.toASTString();
  }

  // Helpers =======================================================================================

  /**
   * Extracts the label number of {@code pStatement}, or returns {@code pIndex} if not applicable.
   */
  private static int getLabelNumberOrIndex(SeqStatement pStatement, int pIndex) {
    return pStatement instanceof SeqThreadStatementClause clause ? clause.labelNumber : pIndex;
  }
}
