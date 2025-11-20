// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildIntegerLiteralExpression;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record ProgramCounterVariableBuilder(
    boolean scalarPc,
    NondeterminismSource nondeterminismSource,
    int numThreads,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  private static final String PROGRAM_COUNTER_VARIABLE_NAME = "pc";

  public static final CIntegerLiteralExpression INIT_PC_LITERAL_EXPRESSION =
      buildIntegerLiteralExpression(ProgramCounterVariables.INIT_PC);

  public static final CIntegerLiteralExpression EXIT_PC_LITERAL_EXPRESSION =
      buildIntegerLiteralExpression(ProgramCounterVariables.EXIT_PC);

  private static final CInitializer INIT_PC_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INIT_PC_LITERAL_EXPRESSION);

  private static final CInitializer EXIT_PC_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, EXIT_PC_LITERAL_EXPRESSION);

  /**
   * A dummy {@link CIdExpression} that can be used to create {@link CArraySubscriptExpression} of
   * {@code pc}, e.g. {@code pc[0]}.
   */
  private static final CIdExpression PC_ARRAY_DUMMY =
      SeqExpressionBuilder.buildIdExpression(
          SeqDeclarationBuilder.buildVariableDeclaration(
              false,
              CArrayType.UNSIGNED_INT_ARRAY,
              PROGRAM_COUNTER_VARIABLE_NAME,
              SeqInitializers.EMPTY_LIST));

  public ProgramCounterVariables buildProgramCounterVariables() throws UnrecognizedCodeException {
    ImmutableList<CLeftHandSide> pcLeftHandSides = buildPcLeftHandSides();
    ImmutableList<CVariableDeclaration> pcDeclarations =
        buildPcDeclarations(numThreads, pcLeftHandSides);
    // pc != 0 (thread is at not exit pc i.e. active)
    ImmutableList<CBinaryExpression> threadActiveExpressions =
        buildPcBinaryExpressions(pcLeftHandSides, BinaryOperator.NOT_EQUALS);
    // pc == 0 (thread is at exit pc i.e. inactive)
    ImmutableList<CBinaryExpression> threadInactiveExpressions =
        buildPcBinaryExpressions(pcLeftHandSides, BinaryOperator.EQUALS);
    // pc[next_thread] != 0, present only if options make it necessary
    Optional<CBinaryExpression> nextThreadActiveExpression = buildNextThreadActiveExpression();
    return new ProgramCounterVariables(
        pcLeftHandSides,
        pcDeclarations,
        threadActiveExpressions,
        threadInactiveExpressions,
        nextThreadActiveExpression);
  }

  // LeftHandSides =================================================================================

  private ImmutableList<CLeftHandSide> buildPcLeftHandSides() {
    ImmutableList.Builder<CLeftHandSide> rPcExpressions = ImmutableList.builder();
    for (int threadId = 0; threadId < numThreads; threadId++) {
      rPcExpressions.add(
          scalarPc ? buildScalarPcExpressions(threadId) : buildArrayPcExpressions(threadId));
    }
    return rPcExpressions.build();
  }

  private CIdExpression buildScalarPcExpressions(int pThreadId) {
    CInitializer initializer = getPcInitializer(pThreadId == 0);
    CVariableDeclaration declaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true,
            CNumericTypes.UNSIGNED_INT,
            PROGRAM_COUNTER_VARIABLE_NAME + pThreadId,
            initializer);
    return new CIdExpression(FileLocation.DUMMY, declaration);
  }

  private CArraySubscriptExpression buildArrayPcExpressions(int pThreadId) {
    return buildPcSubscriptExpression(
        SeqExpressionBuilder.buildIntegerLiteralExpression(pThreadId));
  }

  // Declarations ==================================================================================

  private ImmutableList<CVariableDeclaration> buildPcDeclarations(
      int pNumThreads, ImmutableList<CLeftHandSide> pPcLeftHandSides) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    if (scalarPc) {
      // declare scalar int for each thread: pc0 = 1; pc1 = 0; ...
      for (int i = 0; i < pNumThreads; i++) {
        rDeclarations.add(
            SeqDeclarationBuilder.buildVariableDeclaration(
                true,
                CNumericTypes.UNSIGNED_INT,
                pPcLeftHandSides.get(i).toASTString(),
                getPcInitializer(i == 0)));
      }
    } else {
      // declare int array: pc[] = { 1, 0, ... };
      ImmutableList.Builder<CInitializer> initializers = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        initializers.add(getPcInitializer(i == 0));
      }
      CInitializerList initializerList =
          new CInitializerList(FileLocation.DUMMY, initializers.build());
      rDeclarations.add(
          SeqDeclarationBuilder.buildVariableDeclaration(
              true, CArrayType.UNSIGNED_INT_ARRAY, PROGRAM_COUNTER_VARIABLE_NAME, initializerList));
    }
    return rDeclarations.build();
  }

  // Expressions ===================================================================================

  private Optional<CBinaryExpression> buildNextThreadActiveExpression()
      throws UnrecognizedCodeException {

    if (nondeterminismSource.isNextThreadNondeterministic()) {
      if (!scalarPc) {
        // pc array: single assume(pc[next_thread] != 0);
        return Optional.of(
            binaryExpressionBuilder.buildBinaryExpression(
                buildPcSubscriptExpression(SeqIdExpressions.NEXT_THREAD),
                EXIT_PC_LITERAL_EXPRESSION,
                BinaryOperator.NOT_EQUALS));
      }
    }
    return Optional.empty();
  }

  /** Returns a list of {@code pc{thread_id} != 0} expressions for all {@code pPcLeftHandSides}. */
  private ImmutableList<CBinaryExpression> buildPcBinaryExpressions(
      ImmutableList<CLeftHandSide> pPcLeftHandSides, BinaryOperator pBinaryOperator)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CBinaryExpression> rExpressions = ImmutableList.builder();
    for (CLeftHandSide pcLeftHandSide : pPcLeftHandSides) {
      rExpressions.add(
          binaryExpressionBuilder.buildBinaryExpression(
              pcLeftHandSide, EXIT_PC_LITERAL_EXPRESSION, pBinaryOperator));
    }
    return rExpressions.build();
  }

  private CArraySubscriptExpression buildPcSubscriptExpression(CExpression pSubscriptExpression) {
    return new CArraySubscriptExpression(
        FileLocation.DUMMY, CArrayType.UNSIGNED_INT_ARRAY, PC_ARRAY_DUMMY, pSubscriptExpression);
  }

  // Initializer

  /**
   * Returns the {@link CInitializer} for {@link ProgramCounterVariables#INIT_PC} for the main
   * thread and {@link ProgramCounterVariables#EXIT_PC} for all other threads.
   */
  private static CInitializer getPcInitializer(boolean pIsMainThread) {
    return pIsMainThread ? INIT_PC_INITIALIZER : EXIT_PC_INITIALIZER;
  }
}
