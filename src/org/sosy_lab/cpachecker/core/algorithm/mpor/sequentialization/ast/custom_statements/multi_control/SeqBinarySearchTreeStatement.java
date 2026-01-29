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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;

/** Represents a binary search tree with {@code if-else} branches. */
public record SeqBinarySearchTreeStatement(
    CLeftHandSide expression,
    ImmutableMap<CExportExpression, ? extends CExportStatement> statements,
    CBinaryExpressionBuilder binaryExpressionBuilder)
    implements SeqMultiControlStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner tree = new StringJoiner(SeqSyntax.NEWLINE);
    // use list<entry<,>> instead of map so that we can split it in the middle for the bin tree
    ImmutableList<Entry<CExportExpression, ? extends CExportStatement>> statementList =
        ImmutableList.copyOf(statements.entrySet());
    CExportStatement treeStatement = recursivelyBuildTree(statementList, statementList, expression);
    tree.add(treeStatement.toASTString(pAAstNodeRepresentation));

    return tree.toString();
  }

  /**
   * Recursively builds a binary search tree via {@code if-else} statements for {@code
   * pAllStatements} and returns the resulting root statement.
   */
  private CExportStatement recursivelyBuildTree(
      final ImmutableList<Entry<CExportExpression, ? extends CExportStatement>> pAllStatements,
      List<Entry<CExportExpression, ? extends CExportStatement>> pCurrentStatements,
      CLeftHandSide pPc)
      throws UnrecognizedCodeException {

    int size = pCurrentStatements.size();

    if (size == 1) {
      // single element -> return the statement directly (this is the leaf)
      return pCurrentStatements.getFirst().getValue();

    } else if (size == 2) {
      // only two elements -> create the final if-else leaf statement
      Entry<CExportExpression, ? extends CExportStatement> ifEntry = pCurrentStatements.getFirst();
      CExportStatement elseStatement = pCurrentStatements.getLast().getValue();
      return new CIfStatement(
          ifEntry.getKey(),
          new CCompoundStatement(ifEntry.getValue()),
          new CCompoundStatement(elseStatement));

    } else {
      // more than two elements -> create if and else subtrees with <
      int middleIndex = size / 2;
      Entry<CExportExpression, ? extends CExportStatement> midEntry =
          pCurrentStatements.get(middleIndex);
      int midIndex =
          midEntry.getValue() instanceof SeqThreadStatementClause clause
              ? clause.labelNumber
              : pAllStatements.indexOf(midEntry) - 1;

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
