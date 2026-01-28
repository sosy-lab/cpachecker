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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CExportExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CExportStatement;

public sealed interface SeqMultiControlStatement extends CExportStatement
    permits SeqBinarySearchTreeStatement, SeqIfElseChainStatement, SeqSwitchStatement {

  MultiControlStatementEncoding getEncoding();

  /**
   * Creates the {@link SeqMultiControlStatement} for {@code pThread} based on the specified {@link
   * MultiControlStatementEncoding}.
   */
  static SeqMultiControlStatement buildMultiControlStatementByEncoding(
      MultiControlStatementEncoding pMultiControlStatementEncoding,
      CLeftHandSide pExpression,
      ImmutableList<String> pPrecedingStatements,
      // ImmutableMap retains insertion order when using ImmutableMap.Builder
      ImmutableMap<CExportExpression, ? extends CExportStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    return switch (pMultiControlStatementEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build statements for control encoding " + pMultiControlStatementEncoding);
      case BINARY_SEARCH_TREE ->
          new SeqBinarySearchTreeStatement(
              pExpression, pPrecedingStatements, pStatements, pBinaryExpressionBuilder);
      case IF_ELSE_CHAIN -> new SeqIfElseChainStatement(pPrecedingStatements, pStatements);
      case SWITCH_CASE -> new SeqSwitchStatement(pExpression, pPrecedingStatements, pStatements);
    };
  }
}
