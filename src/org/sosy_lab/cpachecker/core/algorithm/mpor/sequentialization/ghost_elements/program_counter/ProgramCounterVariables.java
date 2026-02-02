// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * Used to store the program counter i.e. {@code pc} and related expressions for each thread. A
 * program counter is used to find the current location of a thread after a context switch.
 *
 * @param pcLeftHandSides The list (indexable by thread IDs) of {@link CLeftHandSide}s for each
 *     thread. This is either an integer {@link CIdExpression} or a {@link
 *     CArraySubscriptExpression} depending on {@link MPOROptions#scalarPc()}.
 * @param pcDeclarations The list (indexable by thread IDs) of {@link CVariableDeclaration} for each
 *     thread. For array {@code pc}, this is a single element.
 * @param threadActiveExpressions The list (indexable by thread IDs) of {@link CBinaryExpression}
 *     that is true when a thread is active, e.g. {@code pc0 != 0}.
 * @param threadInactiveExpressions The list (indexable by thread IDs) of {@link CBinaryExpression}
 *     that is true when a thread is inactive, e.g. {@code pc0 == 0}.
 * @param nextThreadActiveExpression The optional expression to index the {@code next_thread} in the
 *     {@code pc} array.
 */
public record ProgramCounterVariables(
    ImmutableList<CLeftHandSide> pcLeftHandSides,
    ImmutableList<CVariableDeclaration> pcDeclarations,
    ImmutableList<CBinaryExpression> threadActiveExpressions,
    ImmutableList<CBinaryExpression> threadInactiveExpressions,
    Optional<CBinaryExpression> nextThreadActiveExpression) {

  public static final int INIT_PC = 1;

  public static final int EXIT_PC = 0;

  public CLeftHandSide getPcLeftHandSide(int pThreadId) {
    return pcLeftHandSides.get(pThreadId);
  }

  public CBinaryExpression getThreadActiveExpression(int pThreadId) {
    return threadActiveExpressions.get(pThreadId);
  }

  public CBinaryExpression getThreadInactiveExpression(int pThreadId) {
    return threadInactiveExpressions.get(pThreadId);
  }

  /**
   * Returns the {@link CExpressionAssignmentStatement} of {@code pc[pThreadId] = pTargetPc;} or
   * {@code pc{pThreadId} = pTargetPc;} for scalarPc.
   */
  public static CExpressionAssignmentStatement buildPcAssignmentStatement(
      CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY,
        pPcLeftHandSide,
        SeqExpressionBuilder.buildIntegerLiteralExpression(pTargetPc));
  }

  /**
   * Returns the {@link CFunctionCallStatement} of {@code assume(pc[next_thread] != 0);}, i.e. for
   * array {@code pc}.
   */
  public CFunctionCallStatement buildArrayPcUnequalExitPcAssumption() {
    return SeqAssumeFunction.buildAssumeFunctionCallStatement(
        nextThreadActiveExpression.orElseThrow());
  }

  /**
   * Returns the {@link CFunctionCallStatement} of {@code assume(pc{pThread.id} != 0);} i.e. for
   * scalar {@code pc}.
   */
  public CFunctionCallStatement buildScalarPcUnequalExitPcAssumption(MPORThread pThread) {
    CBinaryExpression threadActiveExpression = threadActiveExpressions.get(pThread.id());
    return SeqAssumeFunction.buildAssumeFunctionCallStatement(threadActiveExpression);
  }
}
