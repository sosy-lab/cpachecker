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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MultiControlStatementBuilder {

  /** Creates the {@link SeqMultiControlStatement} for {@code pThread}. */
  public static SeqMultiControlStatement buildMultiControlStatementByEncoding(
      MultiControlStatementEncoding pMultiControlStatementEncoding,
      CLeftHandSide pExpression,
      ImmutableList<String> pPrecedingStatements,
      // ImmutableMap retains insertion order when using ImmutableMap.Builder
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements,
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

  public static ImmutableList<String> buildPrecedingStatements(
      Optional<SeqBranchStatement> pPcUnequalExitAssumption,
      Optional<ImmutableList<String>> pNextThreadAssumption,
      Optional<CFunctionCallAssignmentStatement> pRoundMaxNondetAssignment,
      Optional<SeqBranchStatement> pRoundMaxGreaterZeroAssumption,
      Optional<CExpressionAssignmentStatement> pRoundReset)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rPreceding = ImmutableList.builder();
    if (pPcUnequalExitAssumption.isPresent()) {
      rPreceding.add(pPcUnequalExitAssumption.orElseThrow().toASTString());
    }
    pNextThreadAssumption.ifPresent(rPreceding::addAll);
    pRoundMaxNondetAssignment.ifPresent(s -> rPreceding.add(s.toASTString()));
    if (pRoundMaxGreaterZeroAssumption.isPresent()) {
      rPreceding.add(pRoundMaxGreaterZeroAssumption.orElseThrow().toASTString());
    }
    // place round reset after the assumption for optimization
    pRoundReset.ifPresent(s -> rPreceding.add(s.toASTString()));
    return rPreceding.build();
  }
}
