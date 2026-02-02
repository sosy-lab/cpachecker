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
import com.google.common.collect.Maps;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

/** An abstract base for a multi control flow statement in C such as {@code switch} statements. */
public abstract sealed class CMultiControlStatement implements CExportStatement
    permits CBinarySearchTreeStatement, CIfElseChainStatement, CSwitchStatement {

  final ImmutableListMultimap<CExportExpression, CExportStatement> statements;

  /**
   * Constructor for a {@link CMultiControlStatement}.
   *
   * @param pStatements serves as a common base for all multi control statement, where an expression
   *     is mapped to a list of statements
   */
  CMultiControlStatement(ImmutableListMultimap<CExportExpression, CExportStatement> pStatements) {

    statements = pStatements;
  }

  /**
   * Transforms the statements of this {@link CMultiControlStatement} that are given as a {@link
   * ImmutableListMultimap} into a {@link ImmutableList} that contains {@link Entry}.
   *
   * <p>This is useful for multi control statements that need to index statements by an {@code int},
   * e.g. {@link CBinarySearchTreeStatement} that splits the statements in the middle to build a
   * tree.
   */
  ImmutableList<Entry<CExportExpression, ImmutableList<? extends CExportStatement>>>
      transformStatements() {

    return statements.asMap().entrySet().stream()
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

  /**
   * Creates the {@link CMultiControlStatement} for {@code pThread} based on the specified {@link
   * CMultiControlStatementEncoding}.
   */
  public static CMultiControlStatement buildMultiControlStatementByEncoding(
      CMultiControlStatementEncoding pMultiControlStatementEncoding,
      CLeftHandSide pExpression,
      ImmutableListMultimap<CExportExpression, CExportStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    return switch (pMultiControlStatementEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build statements for control encoding " + pMultiControlStatementEncoding);
      case BINARY_SEARCH_TREE ->
          new CBinarySearchTreeStatement(
              ProgramCounterVariables.INIT_PC, pExpression, pStatements, pBinaryExpressionBuilder);
      case IF_ELSE_CHAIN -> new CIfElseChainStatement(pStatements);
      case SWITCH_CASE -> new CSwitchStatement(pExpression, pStatements);
    };
  }
}
