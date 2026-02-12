// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CSwitchStatement;

/**
 * A builder class for multi selection statements in C, see {@link
 * CMultiSelectionStatementEncoding}.
 */
public class CMultiSelectionStatementBuilder {

  // BINARY_SEARCH_TREE

  /**
   * Returns a new {@link CExportStatement} that represents a {@link
   * CMultiSelectionStatementEncoding#BINARY_SEARCH_TREE}. Example:
   *
   * <pre>{@code
   * if (expr < 3) {
   *   if (expr == 1) { ... }
   *   else { ... }
   * } else {
   *   if (expr == 3) { ... }
   *   else { ... }
   * }
   * }</pre>
   *
   * <p>Note that the indices must be consecutive, i.e., the binary search tree works in the
   * interval {@code [pStartIndex; pStartIndex + pStatements.size() - 1]}.
   *
   * @param pStartIndex the index of the very first statement, e.g. {@code 1} in the example above
   * @param pExpression the {@link CLeftHandSide} used for the binary {@code <} expressions, e.g.
   *     {@code expr} in the example above
   * @param pStatements the map of expressions used in the {@code if} branch (e.g. {@code expr == 1}
   *     in the example above) to their statements
   * @param pBinaryExpressionBuilder the {@link CBinaryExpressionBuilder} used to build the binary
   *     {@code <} expressions
   */
  public static CExportStatement buildBinarySearchTree(
      int pStartIndex,
      CLeftHandSide pExpression,
      ImmutableListMultimap<CExportExpression, CExportStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // use list<entry<,>> instead of map so that we can split it in the middle for the bin tree
    ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
        statementList = transformIntoListWithEntries(pStatements);
    return recursivelyBuildTree(
        pStartIndex, pExpression, statementList, statementList, pBinaryExpressionBuilder);
  }

  /**
   * Recursively builds a binary search tree via {@code if-else} statements for {@code
   * pAllStatements} and returns the resulting root statement.
   */
  private static CExportStatement recursivelyBuildTree(
      final int pStartIndex,
      final CLeftHandSide pExpression,
      final ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
          pAllStatements,
      List<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>> pCurrentStatements,
      final CBinaryExpressionBuilder pBinaryExpressionBuilder)
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
      int midIndex = pAllStatements.indexOf(midEntry);

      // if (pc < midIndex) ...
      CIntegerLiteralExpression intLiteral =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(midIndex + pStartIndex));
      CBinaryExpression ifExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              pExpression, intLiteral, BinaryOperator.LESS_THAN);

      // recursively build if < ...  and else ... subtrees
      CExportStatement ifBranchStatement =
          recursivelyBuildTree(
              pStartIndex,
              pExpression,
              pAllStatements,
              pCurrentStatements.subList(0, middleIndex),
              pBinaryExpressionBuilder);
      CExportStatement elseBranchStatement =
          recursivelyBuildTree(
              pStartIndex,
              pExpression,
              pAllStatements,
              pCurrentStatements.subList(middleIndex, size),
              pBinaryExpressionBuilder);

      return new CIfStatement(
          new CExpressionWrapper(ifExpression),
          new CCompoundStatement(ifBranchStatement),
          new CCompoundStatement(elseBranchStatement));
    }
  }

  // IF_ELSE_CHAIN

  /**
   * Represents a chain of {@code if-else} branches. Example for an {@code int expression} between
   * {@code 0} and {@code 2}:
   *
   * <pre>{@code
   * if (expression == 0) {
   *    ...
   * } else {
   *   if (expression == 1) {
   *      ...
   *   } else {
   *      if (expression == 2) {
   *         ...
   *      }
   *   }
   * }
   * }</pre>
   *
   * <p>For most verifiers, the {@link CMultiSelectionStatementEncoding#IF_ELSE_CHAIN} generally
   * scales much worse with a growing number of statements compared to {@link CSwitchStatement} and
   * {@link CMultiSelectionStatementEncoding#BINARY_SEARCH_TREE}.
   */
  public static CIfStatement buildIfElseChain(
      ImmutableListMultimap<CExportExpression, CExportStatement> pStatements) {

    ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
        statementList = transformIntoListWithEntries(pStatements);

    // start with the very last element (the innermost branch)
    CIfStatement chain =
        new CIfStatement(
            statementList.getLast().getKey(),
            new CCompoundStatement(ImmutableList.copyOf(statementList.getLast().getValue())));

    // wrap it backwards
    for (int i = statementList.size() - 2; i >= 0; i--) {
      Entry<CExportExpression, ImmutableList<? extends CExportStatement>> current =
          statementList.get(i);
      // nest the previous 'if' inside the 'else'
      chain =
          new CIfStatement(
              current.getKey(),
              new CCompoundStatement(ImmutableList.copyOf(current.getValue())),
              new CCompoundStatement(ImmutableList.of(chain)));
    }
    return chain;
  }

  // Helpers

  /**
   * Transforms the statements of this {@code pStatements} that are given as a {@link
   * ImmutableListMultimap} into a {@link ImmutableList} of {@link Entry}s.
   *
   * <p>This is useful for multi control statements that need to index statements by an {@code int},
   * e.g. {@link CMultiSelectionStatementEncoding#BINARY_SEARCH_TREE} that splits the statements in
   * the middle to build a tree.
   */
  private static ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
      transformIntoListWithEntries(
          ImmutableListMultimap<CExportExpression, CExportStatement> pStatements) {

    return pStatements.asMap().entrySet().stream()
        .map(
            entry -> {
              ImmutableList<? extends CExportStatement> values =
                  ImmutableList.copyOf(entry.getValue());
              // explicitly define the types for the Entry to avoid capture errors
              return Maps
                  .<CExportExpression, ImmutableList<? extends CExportStatement>>immutableEntry(
                      entry.getKey(), values);
            })
        .collect(ImmutableList.toImmutableList());
  }
}
