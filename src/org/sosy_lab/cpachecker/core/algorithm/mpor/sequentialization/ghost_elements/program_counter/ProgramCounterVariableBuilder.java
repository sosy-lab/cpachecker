// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record ProgramCounterVariableBuilder(
    MPOROptions options, int numThreads, CBinaryExpressionBuilder binaryExpressionBuilder) {

  public ProgramCounterVariables buildProgramCounterVariables() throws UnrecognizedCodeException {
    ImmutableList<CLeftHandSide> pcLeftHandSides = buildPcLeftHandSides();
    // pc != 0 (thread is at not exit pc i.e. active)
    ImmutableList<CBinaryExpression> threadActiveExpressions =
        buildThreadExpressions(pcLeftHandSides, BinaryOperator.NOT_EQUALS);
    // pc == 0 (thread is at exit pc i.e. inactive)
    ImmutableList<CBinaryExpression> threadInactiveExpressions =
        buildThreadExpressions(pcLeftHandSides, BinaryOperator.EQUALS);
    return new ProgramCounterVariables(
        pcLeftHandSides, threadActiveExpressions, threadInactiveExpressions);
  }

  private ImmutableList<CLeftHandSide> buildPcLeftHandSides() {
    ImmutableList.Builder<CLeftHandSide> rPcExpressions = ImmutableList.builder();
    rPcExpressions.addAll(
        options.scalarPc() ? buildScalarPcExpressions() : buildArrayPcExpressions());
    return rPcExpressions.build();
  }

  // Build Expressions =============================================================================

  private ImmutableList<CIdExpression> buildScalarPcExpressions() {
    ImmutableList.Builder<CIdExpression> rScalarPc = ImmutableList.builder();
    for (int i = 0; i < numThreads; i++) {
      CInitializer initializer = SeqInitializers.getPcInitializer(i == 0);
      CVariableDeclaration declaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              true, CNumericTypes.UNSIGNED_INT, SeqToken.PROGRAM_COUNTER_VARIABLE + i, initializer);
      rScalarPc.add(new CIdExpression(FileLocation.DUMMY, declaration));
    }
    return rScalarPc.build();
  }

  private ImmutableList<CArraySubscriptExpression> buildArrayPcExpressions() {
    ImmutableList.Builder<CArraySubscriptExpression> rArrayPc = ImmutableList.builder();
    for (int i = 0; i < numThreads; i++) {
      rArrayPc.add(
          SeqExpressionBuilder.buildPcSubscriptExpression(
              SeqExpressionBuilder.buildIntegerLiteralExpression(i)));
    }
    return rArrayPc.build();
  }

  /** Returns a list of {@code pc{thread_id} != 0} expressions for all {@code pPcLeftHandSides}. */
  private ImmutableList<CBinaryExpression> buildThreadExpressions(
      ImmutableList<CLeftHandSide> pPcLeftHandSides, BinaryOperator pBinaryOperator)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CBinaryExpression> rExpressions = ImmutableList.builder();
    for (CLeftHandSide pcLeftHandSide : pPcLeftHandSides) {
      rExpressions.add(
          binaryExpressionBuilder.buildBinaryExpression(
              pcLeftHandSide, SeqIntegerLiteralExpressions.INT_EXIT_PC, pBinaryOperator));
    }
    return rExpressions.build();
  }
}
