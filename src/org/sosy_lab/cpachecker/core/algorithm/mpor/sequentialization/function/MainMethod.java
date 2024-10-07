// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.c_to_seq.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.function_call.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MainMethod implements SeqFunction {

  private static final LoopExpr whileExecute = new LoopExpr(SeqIntegerLiteralExpression.INT_1);

  private final CBinaryExpressionBuilder binExprBuilder;

  /** The thread-specific cases in the main while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> loopCases;

  private final CIdExpression numThreads;

  private final CVariableDeclaration declareNumThreads;

  private final CVariableDeclaration declarePc;

  private final CFunctionCallAssignmentStatement assignNextThread;

  private final SeqFunctionCallExpression assumeNextThread;

  private final SeqFunctionCallExpression assumeThreadActive;

  // TODO add an ImmutableSet<CExpression> pAssumptions
  public MainMethod(
      CBinaryExpressionBuilder pBinExprBuilder,
      ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> pLoopCases,
      CIdExpression pNumThreads)
      throws UnrecognizedCodeException {

    binExprBuilder = pBinExprBuilder;
    loopCases = pLoopCases;
    numThreads = pNumThreads;
    declareNumThreads = (CVariableDeclaration) pNumThreads.getDeclaration();
    // TODO declare pc array here
    declarePc = SeqVariableDeclaration.PC;
    assignNextThread =
        new CFunctionCallAssignmentStatement(
            FileLocation.DUMMY,
            SeqIdExpression.NEXT_THREAD,
            new CFunctionCallExpression(
                FileLocation.DUMMY,
                SeqSimpleType.INT,
                SeqIdExpression.VERIFIER_NONDET_INT,
                ImmutableList.of(),
                SeqFunctionDeclaration.VERIFIER_NONDET_INT));
    assumeNextThread =
        new SeqFunctionCallExpression(SeqIdExpression.ASSUME, assumeNextThreadParams());
    assumeThreadActive =
        new SeqFunctionCallExpression(SeqIdExpression.ASSUME, assumeThreadActiveParams());
  }

  @Override
  public String toASTString() {
    StringBuilder switchCases = new StringBuilder();

    int i = 0;
    for (var entry : loopCases.entrySet()) {
      CIntegerLiteralExpression threadId =
          SeqIntegerLiteralExpression.buildIntLiteralExpr(entry.getKey().id);
      IfExpr ifExpr = null;
      try {
        ifExpr =
            new IfExpr(
                binExprBuilder.buildBinaryExpression(
                    SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS));
      } catch (UnrecognizedCodeException pE) {
        throw new RuntimeException(pE);
      }
      // first switch case: use if, otherwise else-if
      if (i == 0) {
        switchCases.append(
            SeqUtil.prependTabsWithoutNewline(2, SeqUtil.appendOpeningCurly(ifExpr.toASTString())));
      } else {
        ElseIfExpr elseIfExpr = new ElseIfExpr(ifExpr);
        switchCases.append(
            SeqUtil.prependTabsWithoutNewline(2, SeqUtil.wrapInCurlyOutwards(elseIfExpr)));
      }
      switchCases.append(SeqSyntax.NEWLINE);
      Builder<String> cases = ImmutableList.builder();
      for (SeqLoopCase loopCase : entry.getValue()) {
        cases.add(loopCase.toASTString());
      }
      CArraySubscriptExpression pcThreadId = SeqExpressions.buildPcSubscriptExpr(threadId);
      SwitchCaseExpr switchCaseExpr = new SwitchCaseExpr(pcThreadId, cases.build(), 3);
      switchCases.append(switchCaseExpr.toASTString());

      // append 2 newlines, except for last switch case (1 only)
      switchCases.append(SeqUtil.repeat(SeqSyntax.NEWLINE, i == loopCases.size() - 1 ? 1 : 2));
      i++;
    }

    return getDeclarationWithParameterNames()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, declareNumThreads.toASTString())
        + SeqUtil.prependTabsWithNewline(1, declarePc.toASTString())
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(whileExecute.toASTString()))
        + SeqUtil.prependTabsWithNewline(2, SeqVariableDeclaration.NEXT_THREAD.toASTString())
        + SeqUtil.prependTabsWithNewline(2, assignNextThread.toASTString())
        + SeqUtil.prependTabsWithNewline(2, assumeNextThread.toASTString() + SeqSyntax.SEMICOLON)
        + SeqUtil.prependTabsWithNewline(2, assumeThreadActive.toASTString() + SeqSyntax.SEMICOLON)
        + SeqSyntax.NEWLINE
        + switchCases
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(
            1,
            SeqToken.RETURN
                + SeqSyntax.SPACE
                + SeqIntegerLiteralExpression.INT_0.toASTString()
                + SeqSyntax.SEMICOLON)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public CType getReturnType() {
    return SeqSimpleType.INT;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpression.MAIN;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    return ImmutableList.of();
  }

  @Override
  public CFunctionDeclaration getDeclaration() {
    return SeqFunctionDeclaration.MAIN;
  }

  private ImmutableList<SeqExpression> assumeNextThreadParams() throws UnrecognizedCodeException {
    Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new SeqLogicalAndExpression(
            binExprBuilder.buildBinaryExpression(
                SeqIntegerLiteralExpression.INT_0,
                SeqIdExpression.NEXT_THREAD,
                BinaryOperator.LESS_EQUAL),
            binExprBuilder.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, numThreads, BinaryOperator.LESS_THAN)));
    return rParams.build();
  }

  private ImmutableList<SeqExpression> assumeThreadActiveParams() throws UnrecognizedCodeException {
    Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new CToSeqExpression(
            binExprBuilder.buildBinaryExpression(
                SeqExpressions.buildPcSubscriptExpr(SeqIdExpression.NEXT_THREAD),
                SeqIntegerLiteralExpression.INT_EXIT_PC,
                BinaryOperator.NOT_EQUALS)));
    return rParams.build();
  }
}
