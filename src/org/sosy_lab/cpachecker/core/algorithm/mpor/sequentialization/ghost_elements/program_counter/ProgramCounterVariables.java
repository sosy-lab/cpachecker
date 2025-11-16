// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

/**
 * Used to store the program counter i.e. {@code pc} and related expressions for each thread. A
 * program counter is used to find the current location of a thread after a context switch.
 *
 * @param programCounter The list (indexable by thread * IDs) of {@link CLeftHandSide}s for each
 *     thread. This is either an integer {@link CIdExpression} or a {@link
 *     CArraySubscriptExpression} depending on {@link MPOROptions#scalarPc()}.
 * @param threadActiveExpressions The list (indexable by thread IDs) of {@link CBinaryExpression}
 *     that is true when a thread is active, e.g. {@code pc0 != 0}.
 * @param threadInactiveExpressions The list (indexable by thread IDs) of {@link CBinaryExpression}
 *     that is true when a thread is inactive, e.g. {@code pc0 == 0}.
 */
public record ProgramCounterVariables(
    ImmutableList<CLeftHandSide> programCounter,
    ImmutableList<CBinaryExpression> threadActiveExpressions,
    ImmutableList<CBinaryExpression> threadInactiveExpressions) {

  public CLeftHandSide getPcLeftHandSide(int pThreadId) {
    return programCounter.get(pThreadId);
  }

  public CBinaryExpression getThreadActiveExpression(int pThreadId) {
    return threadInactiveExpressions.get(pThreadId);
  }

  public CBinaryExpression getThreadInactiveExpression(int pThreadId) {
    return threadInactiveExpressions.get(pThreadId);
  }
}
