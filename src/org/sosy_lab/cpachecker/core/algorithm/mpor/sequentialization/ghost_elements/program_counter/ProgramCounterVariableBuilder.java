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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ProgramCounterVariableBuilder {

  // Program Counter Variables =====================================================================

  public static ProgramCounterVariables buildProgramCounterVariables(
      MPOROptions pOptions, int pNumThreads, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList<CLeftHandSide> pcLeftHandSides = buildPcLeftHandSides(pOptions, pNumThreads);
    ImmutableList<CBinaryExpression> threadNotActiveExpressions =
        SeqExpressionBuilder.buildThreadNotActiveExpressions(
            pcLeftHandSides, pBinaryExpressionBuilder);
    return new ProgramCounterVariables(pcLeftHandSides, threadNotActiveExpressions);
  }

  private static ImmutableList<CLeftHandSide> buildPcLeftHandSides(
      MPOROptions pOptions, int pNumThreads) {

    ImmutableList.Builder<CLeftHandSide> rPcExpressions = ImmutableList.builder();
    rPcExpressions.addAll(
        pOptions.scalarPc
            ? buildScalarPcExpressions(pNumThreads)
            : buildArrayPcExpressions(pNumThreads));
    return rPcExpressions.build();
  }

  // Build Expressions =============================================================================

  private static ImmutableList<CIdExpression> buildScalarPcExpressions(int pNumThreads) {
    ImmutableList.Builder<CIdExpression> rScalarPc = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      CInitializer initializer = SeqInitializer.getPcInitializer(i == 0);
      rScalarPc.add(
          new CIdExpression(
              FileLocation.DUMMY,
              SeqDeclarationBuilder.buildVariableDeclaration(
                  false, SeqSimpleType.UNSIGNED_INT, SeqToken.pc + i, initializer)));
    }
    return rScalarPc.build();
  }

  private static ImmutableList<CArraySubscriptExpression> buildArrayPcExpressions(int pNumThreads) {
    ImmutableList.Builder<CArraySubscriptExpression> rArrayPc = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      rArrayPc.add(
          SeqExpressionBuilder.buildPcSubscriptExpression(
              SeqExpressionBuilder.buildIntegerLiteralExpression(i)));
    }
    return rArrayPc.build();
  }
}
