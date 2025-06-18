// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqElseExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a binary search tree with {@code if-else} branches. */
public class SeqBinaryIfTreeStatement implements SeqMultiControlStatement {

  private final CLeftHandSide expression;

  private final Optional<CFunctionCallStatement> assumption;

  private final Optional<CExpressionAssignmentStatement> lastThreadUpdate;

  private final ImmutableList<? extends SeqStatement> statements;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  SeqBinaryIfTreeStatement(
      CLeftHandSide pExpression,
      Optional<CFunctionCallStatement> pAssumption,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      ImmutableList<? extends SeqStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    expression = pExpression;
    assumption = pAssumption;
    lastThreadUpdate = pLastThreadUpdate;
    statements = pStatements;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> tree = ImmutableList.builder();
    if (assumption.isPresent()) {
      tree.add(LineOfCode.of(assumption.orElseThrow().toASTString()));
    }
    recursivelyBuildTree(statements, statements, expression, tree);
    // TODO the problem here is that with continue; this becomes unreachable -> fix
    if (lastThreadUpdate.isPresent()) {
      tree.add(LineOfCode.of(lastThreadUpdate.orElseThrow().toASTString()));
    }
    return LineOfCodeUtil.buildString(tree.build());
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.BINARY_IF_TREE;
  }

  /**
   * Recursively builds a binary if-else search tree for {@code pStatements} and stores it in {@code
   * pTree}. Note that the labeling does not have to be consecutive from {@code 0}, e.g. {@code 0,
   * 1, 2} or {@code 1, 2, 3} or {@code 0, 2, 3} are all allowed.
   */
  private void recursivelyBuildTree(
      final ImmutableList<? extends SeqStatement> pAllStatements,
      List<? extends SeqStatement> pCurrentStatements,
      CLeftHandSide pPc,
      ImmutableList.Builder<LineOfCode> pTree)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    if (size == 1) {
      // single element -> just place statement without any control flow
      pTree.add(LineOfCode.of(pCurrentStatements.get(0).toASTString().trim()));

    } else if (size == 2) {
      // only two elements -> create if and else leafs with ==
      int low = pAllStatements.indexOf(pCurrentStatements.get(0));
      pTree.add(buildIfEqualsLeaf(pAllStatements, low, pPc));
      pTree.add(buildElseLeaf(pCurrentStatements.get(1)));

    } else {
      // more than two elements -> create if and else subtrees with <
      int mid = size / 2;
      SeqStatement midStatement = pCurrentStatements.get(mid);
      // if statement is a clause, use its label number for the < check
      int midIndex = getLabelNumberOrIndex(midStatement, pAllStatements.indexOf(midStatement)) - 1;

      pTree.add(buildIfSmallerSubtree(midIndex, pPc));
      recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(0, mid), pPc, pTree);
      pTree.add(buildElseSubtree());
      recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(mid, size), pPc, pTree);
      pTree.add(LineOfCode.of(SeqSyntax.CURLY_BRACKET_RIGHT));
    }
  }

  private LineOfCode buildIfSmallerSubtree(int pMid, CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    SeqIfExpression ifSubtree =
        new SeqIfExpression(
            binaryExpressionBuilder.buildBinaryExpression(
                pPc,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pMid + 1),
                BinaryOperator.LESS_THAN));
    return LineOfCode.of(ifSubtree.toASTString() + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT);
  }

  private LineOfCode buildElseSubtree() throws UnrecognizedCodeException {
    SeqElseExpression elseSubtree = new SeqElseExpression();
    return LineOfCode.of(SeqStringUtil.wrapInCurlyBracketsOutwards(elseSubtree.toASTString()));
  }

  private LineOfCode buildIfEqualsLeaf(
      ImmutableList<? extends SeqStatement> pStatements, int pLow, CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    SeqStatement lowStatement = pStatements.get(pLow);
    // if statement is a clause, use its label number as the equals check
    int low = getLabelNumberOrIndex(lowStatement, pLow);
    SeqIfExpression ifLeaf =
        new SeqIfExpression(
            binaryExpressionBuilder.buildBinaryExpression(
                pPc,
                SeqExpressionBuilder.buildIntegerLiteralExpression(low),
                BinaryOperator.EQUALS));
    return buildLeaf(ifLeaf, lowStatement);
  }

  private LineOfCode buildElseLeaf(SeqStatement pHighStatement) throws UnrecognizedCodeException {

    SeqElseExpression elseLeaf = new SeqElseExpression();
    return buildLeaf(elseLeaf, pHighStatement);
  }

  private LineOfCode buildLeaf(
      SeqSingleControlExpression pSingleControlStatement, SeqStatement pStatement)
      throws UnrecognizedCodeException {

    String prefix = pSingleControlStatement.toASTString() + SeqSyntax.SPACE;
    String code = SeqStringUtil.wrapInCurlyBracketsInwards(pStatement.toASTString());
    return LineOfCode.of(prefix + code);
  }

  // Helpers =======================================================================================

  /**
   * Extracts the label number of {@code pStatement}, or returns {@code pIndex} if not applicable.
   */
  private static int getLabelNumberOrIndex(SeqStatement pStatement, int pIndex) {
    return pStatement instanceof SeqThreadStatementClause
        ? ((SeqThreadStatementClause) pStatement).labelNumber
        : pIndex;
  }
}
