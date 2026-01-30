// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
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

/**
 * Represents a binary search tree with {@code if-else} branches. Example for an {@code int
 * expression} between {@code 1} and {@code 4}:
 *
 * <pre>{@code
 * if (expression < 3) {
 *   if (expression == 1) { ... }
 *   else { ... }
 * } else {
 *   if (expression == 3) { ... }
 *   else { ... }
 * }
 * }</pre>
 *
 * <p>In tests with CBMC, the {@link CBinarySearchTreeStatement} scaled much better with a growing
 * number of statements compared to {@link CSwitchStatement} and {@link CIfElseChainStatement}. For
 * other verifiers, the impact was negligible.
 */
public final class CBinarySearchTreeStatement extends CMultiControlStatement {

  private final int startIndex;

  private final CLeftHandSide expression;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  /**
   * Returns a new {@link CBinarySearchTreeStatement}. Example:
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
  public CBinarySearchTreeStatement(
      int pStartIndex,
      CLeftHandSide pExpression,
      ImmutableListMultimap<CExportExpression, ? extends CExportStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    super(pStatements);
    startIndex = pStartIndex;
    expression = pExpression;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    // use list<entry<,>> instead of map so that we can split it in the middle for the bin tree
    ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
        statementList = transformStatements();
    CExportStatement treeStatement = recursivelyBuildTree(statementList, statementList);
    return treeStatement.toASTString(pAAstNodeRepresentation);
  }

  /**
   * Recursively builds a binary search tree via {@code if-else} statements for {@code
   * pAllStatements} and returns the resulting root statement.
   */
  private CExportStatement recursivelyBuildTree(
      final ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
          pAllStatements,
      List<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>> pCurrentStatements)
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
              FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(midIndex + startIndex));
      CBinaryExpression ifExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              expression, intLiteral, BinaryOperator.LESS_THAN);

      // recursively build if < ...  and else ... subtrees
      CExportStatement ifBranchStatement =
          recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(0, middleIndex));
      CExportStatement elseBranchStatement =
          recursivelyBuildTree(pAllStatements, pCurrentStatements.subList(middleIndex, size));

      return new CIfStatement(
          new CExpressionWrapper(ifExpression),
          new CCompoundStatement(ifBranchStatement),
          new CCompoundStatement(elseBranchStatement));
    }
  }
}
