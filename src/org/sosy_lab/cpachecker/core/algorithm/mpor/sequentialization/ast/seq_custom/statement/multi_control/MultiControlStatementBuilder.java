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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadEndLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class MultiControlStatementBuilder {

  /** Creates the {@link SeqMultiControlStatement} for {@code pThread}. */
  public static SeqMultiControlStatement buildMultiControlStatementByEncoding(
      MPOROptions pOptions,
      MultiControlStatementEncoding pMultiControlStatementEncoding,
      CLeftHandSide pExpression,
      ImmutableList<CStatement> pPrecedingStatements,
      // ImmutableMap retains insertion order when using ImmutableMap.Builder
      ImmutableMap<CExpression, ? extends SeqStatement> pStatements,
      Optional<SeqThreadEndLabelStatement> pThreadEndLabel,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    // TODO add default error statement for binary tree and if-else chain
    return switch (pMultiControlStatementEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build statements for control encoding " + pMultiControlStatementEncoding);
      case BINARY_IF_TREE ->
          new SeqBinaryIfTreeStatement(
              pExpression,
              pPrecedingStatements,
              pStatements,
              pThreadEndLabel,
              pLastThreadUpdate,
              pBinaryExpressionBuilder);
      case IF_ELSE_CHAIN ->
          new SeqIfElseChainStatement(
              pPrecedingStatements, pStatements, pThreadEndLabel, pLastThreadUpdate);
      case SWITCH_CASE ->
          new SeqSwitchStatement(
              pOptions, pExpression, pPrecedingStatements, pLastThreadUpdate, pStatements);
    };
  }

  static ImmutableList<LineOfCode> buildThreadEndLabel(
      Optional<SeqThreadEndLabelStatement> pThreadEndLabel,
      Optional<CExpressionAssignmentStatement> pLastThreadUpdate) {

    ImmutableList.Builder<LineOfCode> rLines = ImmutableList.builder();
    if (pThreadEndLabel.isPresent()) {
      rLines.add(LineOfCode.of(pThreadEndLabel.orElseThrow().toASTString()));
      if (pLastThreadUpdate.isEmpty()) {
        // add empty statement ';', otherwise the C Parser rejects the program
        rLines.add(LineOfCode.of(SeqSyntax.SEMICOLON));
      }
    }
    return rLines.build();
  }

  public static ImmutableList<CStatement> buildPrecedingStatements(
      Optional<CFunctionCallStatement> pThreadActiveAssumption,
      Optional<CFunctionCallAssignmentStatement> pKNondet,
      Optional<CFunctionCallStatement> pKGreaterZeroAssumption,
      Optional<CFunctionCallStatement> pKBoundAssumption,
      Optional<CExpressionAssignmentStatement> pRReset) {

    ImmutableList.Builder<CStatement> rPreceding = ImmutableList.builder();
    if (pThreadActiveAssumption.isPresent()) {
      rPreceding.add(pThreadActiveAssumption.orElseThrow());
    }
    if (pKNondet.isPresent()) {
      rPreceding.add(pKNondet.orElseThrow());
    }
    if (pKGreaterZeroAssumption.isPresent()) {
      rPreceding.add(pKGreaterZeroAssumption.orElseThrow());
    }
    if (pKBoundAssumption.isPresent()) {
      rPreceding.add(pKBoundAssumption.orElseThrow());
    }
    // place r reset after the assumption for optimization
    if (pRReset.isPresent()) {
      rPreceding.add(pRReset.orElseThrow());
    }
    return rPreceding.build();
  }
}
