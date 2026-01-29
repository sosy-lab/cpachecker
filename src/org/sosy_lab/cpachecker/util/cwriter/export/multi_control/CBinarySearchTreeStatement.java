// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;

/** Represents a binary search tree with {@code if-else} branches. */
public final class CBinarySearchTreeStatement extends CMultiControlStatement {

  private final CLeftHandSide expression;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public CBinarySearchTreeStatement(
      CLeftHandSide pExpression,
      ImmutableListMultimap<CExportExpression, ? extends CExportStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    super(pStatements);
    expression = pExpression;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    // use list<entry<,>> instead of map so that we can split it in the middle for the bin tree
    ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
        statementList = transformStatements();
    CExportStatement treeStatement = recursivelyBuildTree(statementList, statementList, expression);
    return treeStatement.toASTString(pAAstNodeRepresentation);
  }

  /**
   * Recursively builds a binary search tree via {@code if-else} statements for {@code
   * pAllStatements} and returns the resulting root statement.
   */
  private CExportStatement recursivelyBuildTree(
      final ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
          pAllStatements,
      List<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>> pCurrentStatements,
      CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    if (size == 1) {
      // single element -> return the statement directly (this is the leaf)
      return new CCompoundStatement(ImmutableList.copyOf(pCurrentStatements.getFirst().getValue()));

    } else if (size == 2) {
      // only two elements -> create the final if-else leaf statement
      Entry<CExportExpression, ImmutableList<? extends CExportStatement>> ifEntry =
          pCurrentStatements.getFirst();
      ImmutableList<? extends CExportStatement> elseStatements =
          pCurrentStatements.getLast().getValue();
      return new CIfStatement(
          ifEntry.getKey(),
          new CCompoundStatement(ImmutableList.copyOf(ifEntry.getValue())),
          new CCompoundStatement(ImmutableList.copyOf(elseStatements)));

    } else {
      // more than two elements -> create if and else subtrees with <
      int middleIndex = size / 2;
      Entry<CExportExpression, ImmutableList<? extends CExportStatement>> midEntry =
          pCurrentStatements.get(middleIndex);
      int midIndex = pAllStatements.indexOf(midEntry) - 1;

      // if (pc < midIndex) ...
      CBinaryExpression ifExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              pPc,
              SeqExpressionBuilder.buildIntegerLiteralExpression(midIndex + 1),
              BinaryOperator.LESS_THAN);

      // recursively build if < ...  and else ... subtrees
      CExportStatement ifBranchStatement =
          recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(0, middleIndex), pPc);
      CExportStatement elseBranchStatement =
          recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(middleIndex, size), pPc);

      return new CIfStatement(
          new CExpressionWrapper(ifExpression),
          new CCompoundStatement(ifBranchStatement),
          new CCompoundStatement(elseBranchStatement));
    }
  }
}
