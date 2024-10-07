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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.InitializerListExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.LoopExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.function_call.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqValue;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MainMethod implements SeqFunction {

  private static final LoopExpr whileExecute = new LoopExpr(SeqExpressions.INT_1);

  private final CBinaryExpressionBuilder binExprBuilder;

  /** The thread-specific cases in the main while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqLoopCase>> loopCases;

  private final CIdExpression numThreads;

  private final CVariableDeclaration declareNumThreads;

  private final CVariableDeclaration declarePc;

  private final CVariableDeclaration declareNextThread;

  private final CFunctionCallAssignmentStatement assignNextThread;

  private final FunctionCallExpr assumeNextThread;

  private final IfExpr exitPcCheck;

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
    declarePc = SeqDeclarations.PC;
    declareNextThread = SeqDeclarations.NEXT_THREAD;
    assignNextThread =
        new CFunctionCallAssignmentStatement(
            FileLocation.DUMMY,
            SeqExpressions.NEXT_THREAD,
            new CFunctionCallExpression(
                FileLocation.DUMMY,
                SeqTypes.INT,
                SeqExpressions.VERIFIER_NONDET_INT,
                ImmutableList.of(),
                SeqDeclarations.VERIFIER_NONDET_INT));
    assumeNextThread =
        new FunctionCallExpr(
            SeqNameBuilder.createFuncName(SeqToken.ASSUME), assumeNextThreadParams());
    exitPcCheck =
        new IfExpr(
            binExprBuilder.buildBinaryExpression(
                SeqExpressions.buildPcSubscriptExpr(SeqExpressions.NEXT_THREAD),
                SeqExpressions.INT_EXIT_PC,
                BinaryOperator.EQUALS));
  }

  @Override
  public String toASTString() {
    StringBuilder switchCases = new StringBuilder();

    int i = 0;
    for (var entry : loopCases.entrySet()) {
      CIntegerLiteralExpression threadId = SeqExpressions.buildIntLiteralExpr(entry.getKey().id);
      IfExpr ifExpr = null;
      try {
        ifExpr =
            new IfExpr(
                binExprBuilder.buildBinaryExpression(
                    SeqExpressions.NEXT_THREAD, threadId, BinaryOperator.EQUALS));
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
      ArrayElement arrayElem = new ArrayElement(SeqVars.pc, threadId);
      SwitchCaseExpr switchCaseExpr = new SwitchCaseExpr(arrayElem, cases.build(), 3);
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
        + SeqUtil.prependTabsWithNewline(2, declareNextThread.toASTString())
        + SeqUtil.prependTabsWithNewline(2, assignNextThread.toASTString())
        + SeqUtil.prependTabsWithNewline(2, assumeNextThread.toASTString() + SeqSyntax.SEMICOLON)
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(2, SeqUtil.appendOpeningCurly(exitPcCheck.toASTString()))
        + SeqUtil.prependTabsWithNewline(3, SeqToken.CONTINUE + SeqSyntax.SEMICOLON)
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqSyntax.NEWLINE
        + switchCases
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(
            1, SeqToken.RETURN + SeqSyntax.SPACE + SeqExpressions.INT_0.toASTString())
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public CType getReturnType() {
    return SeqTypes.INT;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqExpressions.MAIN;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    return ImmutableList.of();
  }

  @Override
  public CFunctionDeclaration getDeclaration() {
    return SeqDeclarations.MAIN;
  }

  private InitializerListExpr pcInitializerList(int pNumThreads) {
    Builder<SeqExpression> rInitializers = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      rInitializers.add(new Value(SeqValue.ZERO));
    }
    return new InitializerListExpr(rInitializers.build());
  }

  private ImmutableList<SeqExpression> assumeNextThreadParams() throws UnrecognizedCodeException {
    Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new SeqLogicalAndExpression(
            binExprBuilder.buildBinaryExpression(
                SeqExpressions.INT_0, SeqExpressions.NEXT_THREAD, BinaryOperator.LESS_EQUAL),
            binExprBuilder.buildBinaryExpression(
                SeqExpressions.NEXT_THREAD, numThreads, BinaryOperator.LESS_THAN)));
    return rParams.build();
  }

  private static ImmutableList<SeqExpression> anyNonNegativeParams() {
    Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(SeqVars.pc);
    rParams.add(SeqVars.numThreads);
    return rParams.build();
  }
}
