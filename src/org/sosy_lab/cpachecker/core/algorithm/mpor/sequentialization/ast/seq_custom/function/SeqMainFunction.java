// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializerList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction implements SeqFunction {

  private static final SeqControlFlowStatement whileTrue =
      new SeqControlFlowStatement(
          SeqIntegerLiteralExpression.INT_1, SeqControlFlowStatementType.WHILE);

  private final CIdExpression numThreads;

  private final ImmutableList<SeqLogicalAndExpression> threadAssertions;

  private final ImmutableList<SeqFunctionCallExpression> threadAssumptions;

  private final ImmutableList<SeqFunctionCallExpression> porAssumptions;

  /** The thread-specific case clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses;

  private final CVariableDeclaration declarePc;

  private final CFunctionCallAssignmentStatement assignNextThread;

  private final CExpressionAssignmentStatement assignPrevThread;

  private final SeqFunctionCallExpression assumeNextThread;

  private final SeqFunctionCallExpression assumeThreadActive;

  public SeqMainFunction(
      int pNumThreads,
      ImmutableList<SeqLogicalAndExpression> pThreadAssertions,
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions,
      ImmutableList<SeqFunctionCallExpression> pPORAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses)
      throws UnrecognizedCodeException {

    numThreads =
        SeqIdExpression.buildIdExpr(
            SeqVariableDeclaration.buildVarDec(
                false,
                SeqSimpleType.CONST_INT,
                SeqToken.NUM_THREADS,
                SeqInitializer.buildIntInitializer(
                    SeqIntegerLiteralExpression.buildIntLiteralExpr(pNumThreads))));
    threadAssertions = pThreadAssertions;
    threadAssumptions = pThreadAssumptions;
    porAssumptions = pPORAssumptions;
    caseClauses = pCaseClauses;

    CInitializerList pcInitializerList =
        SeqInitializerList.buildIntInitializerList(SeqIntegerLiteralExpression.INT_0, pNumThreads);
    declarePc =
        SeqVariableDeclaration.buildVarDec(
            false, SeqArrayType.INT_ARRAY, SeqToken.PC, pcInitializerList);

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

    assignPrevThread =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, SeqIdExpression.PREV_THREAD, SeqIdExpression.NEXT_THREAD);

    assumeNextThread =
        new SeqFunctionCallExpression(SeqIdExpression.ASSUME, assumeNextThreadParams());
    assumeThreadActive =
        new SeqFunctionCallExpression(SeqIdExpression.ASSUME, assumeThreadActiveParams());
  }

  @Override
  public String toASTString() {
    // create assertion checks
    StringBuilder assertions = new StringBuilder();
    String seqError = Sequentialization.getSeqErrorFunctionCall();
    for (SeqLogicalAndExpression assertion : threadAssertions) {
      SeqControlFlowStatement ifStmt =
          new SeqControlFlowStatement(assertion, SeqControlFlowStatementType.IF);
      assertions.append(
          SeqUtil.prependTabsWithNewline(2, SeqUtil.appendOpeningCurly(ifStmt.toASTString())));
      assertions.append(SeqUtil.prependTabsWithNewline(3, SeqUtil.appendClosingCurly(seqError)));
    }

    // create assume call strings
    StringBuilder assumptions = new StringBuilder();
    for (SeqFunctionCallExpression assumption : threadAssumptions) {
      String assumeStatement = assumption.toASTString() + SeqSyntax.SEMICOLON;
      assumptions.append(SeqUtil.prependTabsWithNewline(2, assumeStatement));
    }
    assumptions.append(SeqSyntax.NEWLINE);
    for (SeqFunctionCallExpression assumption : porAssumptions) {
      String assumeStatement = assumption.toASTString() + SeqSyntax.SEMICOLON;
      assumptions.append(SeqUtil.prependTabsWithNewline(2, assumeStatement));
    }

    // create switch statement string
    StringBuilder switches = new StringBuilder();
    int i = 0;
    for (var entry : caseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CIntegerLiteralExpression threadId =
          SeqIntegerLiteralExpression.buildIntLiteralExpr(thread.id);
      try {
        CBinaryExpression nextThreadEquals =
            SeqBinaryExpression.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS);
        // first switch case: use "if", otherwise "else if"
        SeqControlFlowStatementType stmtType =
            i == 0 ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
        SeqControlFlowStatement stmt = new SeqControlFlowStatement(nextThreadEquals, stmtType);
        switches.append(
            SeqUtil.prependTabsWithoutNewline(
                2,
                i == 0
                    ? SeqUtil.appendOpeningCurly(stmt.toASTString())
                    : SeqUtil.wrapInCurlyOutwards(stmt.toASTString())));
      } catch (UnrecognizedCodeException e) {
        throw new RuntimeException(e);
      }
      switches.append(SeqSyntax.NEWLINE);
      CArraySubscriptExpression pcThreadId = SeqExpressions.buildPcSubscriptExpr(threadId);
      SeqSwitchStatement switchCaseExpr = new SeqSwitchStatement(pcThreadId, entry.getValue(), 3);
      switches.append(switchCaseExpr.toASTString());

      // append 2 newlines, except for last switch case (1 only)
      switches.append(SeqUtil.repeat(SeqSyntax.NEWLINE, i == caseClauses.size() - 1 ? 1 : 2));
      i++;
    }
    return getDeclarationWithParameterNames()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, numThreads.getDeclaration().toASTString())
        + SeqUtil.prependTabsWithNewline(1, declarePc.toASTString())
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqVariableDeclaration.PREV_THREAD.toASTString())
        + SeqUtil.prependTabsWithNewline(1, SeqVariableDeclaration.NEXT_THREAD.toASTString())
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(whileTrue.toASTString()))
        + assertions
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(2, assignNextThread.toASTString())
        + SeqUtil.prependTabsWithNewline(2, assumeNextThread.toASTString() + SeqSyntax.SEMICOLON)
        + SeqUtil.prependTabsWithNewline(2, assumeThreadActive.toASTString() + SeqSyntax.SEMICOLON)
        + SeqSyntax.NEWLINE
        + assumptions
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(2, assignPrevThread.toASTString())
        + SeqSyntax.NEWLINE
        + switches
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        // TODO assert_fail instead of return 0? the return statement should never be reached
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
    ImmutableList.Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new SeqLogicalAndExpression(
            SeqBinaryExpression.buildBinaryExpression(
                SeqIntegerLiteralExpression.INT_0,
                SeqIdExpression.NEXT_THREAD,
                BinaryOperator.LESS_EQUAL),
            SeqBinaryExpression.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, numThreads, BinaryOperator.LESS_THAN)));
    return rParams.build();
  }

  private ImmutableList<SeqExpression> assumeThreadActiveParams() throws UnrecognizedCodeException {
    ImmutableList.Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new CToSeqExpression(
            SeqBinaryExpression.buildBinaryExpression(
                SeqExpressions.buildPcSubscriptExpr(SeqIdExpression.NEXT_THREAD),
                SeqIntegerLiteralExpression.INT_EXIT_PC,
                BinaryOperator.NOT_EQUALS)));
    return rParams.build();
  }
}
