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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a binary search tree with {@code if-else} branches. */
public record SeqBinarySearchTreeStatement(
    CLeftHandSide expression,
    ImmutableList<String> precedingStatements,
    ImmutableMap<CExpression, ? extends SeqStatement> statements,
    CBinaryExpressionBuilder binaryExpressionBuilder)
    implements SeqMultiControlStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner tree = new StringJoiner(SeqSyntax.NEWLINE);
    precedingStatements.forEach(statement -> tree.add(statement));
    // use list<entry<,>> instead of map so that we can split it in the middle for the bin tree
    ImmutableList<Entry<CExpression, ? extends SeqStatement>> statementList =
        ImmutableList.copyOf(statements.entrySet());
    SeqStatement treeStatement = recursivelyBuildTree(statementList, statementList, expression);
    tree.add(treeStatement.toASTString());
    return tree.toString();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.BINARY_SEARCH_TREE;
  }

  /**
   * Recursively builds a binary search tree via {@code if-else} statements for {@code
   * pAllStatements} and returns the resulting root statement.
   */
  private SeqStatement recursivelyBuildTree(
      final ImmutableList<Entry<CExpression, ? extends SeqStatement>> pAllStatements,
      List<Entry<CExpression, ? extends SeqStatement>> pCurrentStatements,
      CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    if (size == 1) {
      // single element -> return the statement directly (this is the leaf)
      return pCurrentStatements.getFirst().getValue();

    } else if (size == 2) {
      // only two elements -> create the final if-else leaf statement
      Entry<CExpression, ? extends SeqStatement> ifEntry = pCurrentStatements.getFirst();
      SeqStatement elseStatement = pCurrentStatements.getLast().getValue();
      return buildIfElseLeaf(ifEntry.getKey(), ifEntry.getValue(), elseStatement);

    } else {
      // more than two elements -> create if and else subtrees with <
      int middleIndex = size / 2;
      Entry<CExpression, ? extends SeqStatement> midEntry = pCurrentStatements.get(middleIndex);
      SeqStatement midStatement = midEntry.getValue();
      int labelIndex = getLabelNumberOrIndex(midStatement, pAllStatements.indexOf(midEntry)) - 1;
      CBinaryExpression ifExpression = buildIfSmallerExpression(labelIndex, pPc);

      // recursively build if < ...  and else ... subtrees
      SeqStatement ifBranchStatement =
          recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(0, middleIndex), pPc);
      SeqStatement elseBranchStatement =
          recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(middleIndex, size), pPc);

      return new SeqBranchStatement(
          ifExpression.toASTString(),
          ImmutableList.of(ifBranchStatement.toASTString()),
          ImmutableList.of(elseBranchStatement.toASTString()));
    }
  }

  private CBinaryExpression buildIfSmallerExpression(int pMiddleIndex, CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    return binaryExpressionBuilder.buildBinaryExpression(
        pPc,
        SeqExpressionBuilder.buildIntegerLiteralExpression(pMiddleIndex + 1),
        BinaryOperator.LESS_THAN);
  }

  private SeqBranchStatement buildIfElseLeaf(
      CExpression pIfExpression, SeqStatement pIfBranchStatement, SeqStatement pElseIfStatement)
      throws UnrecognizedCodeException {

    return new SeqBranchStatement(
        pIfExpression.toASTString(),
        ImmutableList.of(pIfBranchStatement.toASTString()),
        ImmutableList.of(pElseIfStatement.toASTString()));
  }

  // Helpers =======================================================================================

  /**
   * Extracts the label number of {@code pStatement}, or returns {@code pIndex} if not applicable.
   */
  private static int getLabelNumberOrIndex(SeqStatement pStatement, int pIndex) {
    return pStatement instanceof SeqThreadStatementClause clause ? clause.labelNumber : pIndex;
  }
}
