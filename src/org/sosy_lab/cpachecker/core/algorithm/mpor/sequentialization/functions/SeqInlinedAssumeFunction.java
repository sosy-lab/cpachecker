// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

/**
 * Represents an inlined call to an assume function, e.g. {@code assume(condition);} is inlined to
 * {@code if (condition == 0) { abort(); }}. This is done for performance reasons, because the
 * assume function can be called extremely often, depending on the state space of the input program.
 *
 * <p>When inlining the function, the backend verification tool does not have to deal with call and
 * return contexts as well as parameters.
 */
public final class SeqInlinedAssumeFunction {

  // CFunctionType

  private static final CFunctionType ABORT_FUNCTION_TYPE =
      new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);

  // CFunctionDeclaration

  public static final CFunctionDeclaration ABORT_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          ABORT_FUNCTION_TYPE,
          "abort",
          ImmutableList.of(),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  // CFunctionCallExpression

  private static final CFunctionCallExpression ABORT_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          new CIdExpression(FileLocation.DUMMY, ABORT_FUNCTION_DECLARATION),
          ImmutableList.of(),
          ABORT_FUNCTION_DECLARATION);

  // CFunctionCallStatement

  public static final CFunctionCallStatement ABORT_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, ABORT_FUNCTION_CALL_EXPRESSION);

  private static final ImmutableList<String> ABORT_BRANCH_STATEMENT =
      ImmutableList.of(ABORT_FUNCTION_CALL_STATEMENT.toASTString());

  /** Returns an inlined call to assume i.e. {@code if (pCondition == 0) { abort(); }} */
  public static SeqBranchStatement buildInlinedAssumeFunctionCall(CExpression pCondition) {
    return new SeqBranchStatement(pCondition.toASTString() + " == 0", ABORT_BRANCH_STATEMENT);
  }

  /** Returns an inlined call to assume i.e. {@code if (pCondition == 0) { abort(); }} */
  public static SeqBranchStatement buildInlinedAssumeFunctionCall(
      ExpressionTree<CExpression> pCondition) {

    return new SeqBranchStatement(pCondition + " == 0", ABORT_BRANCH_STATEMENT);
  }

  /**
   * Returns the function call to assume as a {@link String} that restricts the nondeterministic
   * {@code next_thread} variable to an appropriate value, i.e. {@code assume(0 <= next_thread &&
   * next_thread < NUM_THREADS)} for a signed variable, {@code assume(next_thread < NUM_THREADS)}
   * for an unsigned variable.
   */
  public static SeqBranchStatement buildNextThreadAssumeCallFunctionCallStatement(
      boolean pIsSigned, int pNumThreads, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression numThreadsExpression =
        SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads);
    // ensure that next_thread < NUM_THREADS
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.NEXT_THREAD, numThreadsExpression, BinaryOperator.LESS_THAN);

    // if next_thread is signed, then we also need to ensure that 0 <= next_thread
    if (pIsSigned) {
      CBinaryExpression nextThreadGreaterOrEqualZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIntegerLiteralExpressions.INT_0,
              SeqIdExpressions.NEXT_THREAD,
              BinaryOperator.LESS_EQUAL);
      ImmutableList<CBinaryExpression> expressions =
          ImmutableList.of(nextThreadLessThanNumThreads, nextThreadGreaterOrEqualZero);
      return buildInlinedAssumeFunctionCall(
          And.of(transformedImmutableListCopy(expressions, LeafExpression::of)));
    }
    return buildInlinedAssumeFunctionCall(nextThreadLessThanNumThreads);
  }
}
