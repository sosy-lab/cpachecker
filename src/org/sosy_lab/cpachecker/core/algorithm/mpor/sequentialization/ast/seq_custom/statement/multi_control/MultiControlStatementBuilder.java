// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;

public class MultiControlStatementBuilder {

  /** Creates the {@link SeqMultiControlStatement} for {@code pThread}. */
  public static SeqMultiControlStatement buildMultiControlStatementByEncoding(
      MPOROptions pOptions,
      MultiControlStatementEncoding pMultiControlStatementEncoding,
      CLeftHandSide pExpression,
      Optional<CFunctionCallStatement> pAssumption,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      ImmutableList<? extends SeqStatement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    return switch (pMultiControlStatementEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build statements for control encoding " + pMultiControlStatementEncoding);
      case BINARY_IF_TREE ->
          new SeqBinaryIfTreeStatement(
              pExpression, pAssumption, pLastThreadUpdate, pStatements, pBinaryExpressionBuilder);
      case IF_ELSE_CHAIN ->
          new SeqIfElseChainStatement(
              pExpression,
              Sequentialization.INIT_PC,
              pAssumption,
              pLastThreadUpdate,
              pStatements,
              pBinaryExpressionBuilder);
      case SWITCH_CASE ->
          new SeqSwitchStatement(
              pOptions, pExpression, pAssumption, pLastThreadUpdate, pStatements);
    };
  }
}
