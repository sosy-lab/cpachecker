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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqElseExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadEndLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a binary search tree with {@code if-else} branches. */
public class SeqBinaryIfTreeStatement implements SeqMultiControlStatement {

  private final CLeftHandSide expression;

  private final ImmutableList<CStatement> precedingStatements;

  private final ImmutableMap<CExpression, ? extends SeqStatement> statements;

  private final Optional<SeqThreadEndLabelStatement> threadEndLabel;

  private final Optional<CExpressionAssignmentStatement> lastThreadUpdate;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  SeqBinaryIfTreeStatement(
      CLeftHandSide pExpression,
      ImmutableList<CStatement> pPrecedingStatements,
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements,
      Optional<SeqThreadEndLabelStatement> pThreadEndLabel,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    expression = pExpression;
    precedingStatements = pPrecedingStatements;
    statements = pStatements;
    threadEndLabel = pThreadEndLabel;
    lastThreadUpdate = pLastThreadUpdate;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> tree = ImmutableList.builder();
    tree.addAll(LineOfCodeUtil.buildLinesOfCodeFromCAstNodes(precedingStatements));
    ImmutableList<Map.Entry<CExpression, ? extends SeqStatement>> statementList =
        ImmutableList.copyOf(statements.entrySet());
    recursivelyBuildTree(statementList, statementList, expression, tree);
    tree.addAll(MultiControlStatementBuilder.buildThreadEndLabel(threadEndLabel, lastThreadUpdate));
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
   * Recursively builds a binary if-else search tree for {@code pAllStatements} and stores it in
   * {@code pTree}. Note that the labeling does not have to be consecutive from {@code 0}, e.g.
   * {@code 0, 1, 2} or {@code 1, 2, 3} or {@code 0, 2, 3} are all allowed.
   */
  private void recursivelyBuildTree(
      final ImmutableList<Map.Entry<CExpression, ? extends SeqStatement>> pAllStatements,
      List<Map.Entry<CExpression, ? extends SeqStatement>> pCurrentStatements,
      CLeftHandSide pPc,
      ImmutableList.Builder<LineOfCode> pTree)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    // TODO actually use the CExpression keys
    if (size == 1) {
      // single element -> just place statement without any control flow
      pTree.add(LineOfCode.of(pCurrentStatements.getFirst().getValue().toASTString()));

    } else if (size == 2) {
      // only two elements -> create if and else leafs with ==
      int low = pAllStatements.indexOf(pCurrentStatements.getFirst());
      pTree.add(buildIfEqualsLeaf(pAllStatements.get(low).getValue(), low, pPc));
      pTree.add(buildElseLeaf(pCurrentStatements.get(1).getValue()));

    } else {
      // more than two elements -> create if and else subtrees with <
      int mid = size / 2;
      Map.Entry<CExpression, ? extends SeqStatement> midEntry = pCurrentStatements.get(mid);
      SeqStatement midStatement = midEntry.getValue();
      // if statement is a clause, use its label number for the < check
      int midIndex = getLabelNumberOrIndex(midStatement, pAllStatements.indexOf(midEntry)) - 1;

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

  private LineOfCode buildIfEqualsLeaf(SeqStatement pStatement, int pLow, CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    // if statement is a clause, use its label number as the equals check
    int low = getLabelNumberOrIndex(pStatement, pLow);
    SeqIfExpression ifLeaf =
        new SeqIfExpression(
            binaryExpressionBuilder.buildBinaryExpression(
                pPc,
                SeqExpressionBuilder.buildIntegerLiteralExpression(low),
                BinaryOperator.EQUALS));
    return buildLeaf(ifLeaf, pStatement);
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
    return pStatement instanceof SeqThreadStatementClause clause ? clause.labelNumber : pIndex;
  }
}
