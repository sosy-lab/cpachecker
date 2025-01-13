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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqArraySubscriptExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqScalarPcAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction implements SeqFunction {

  private static final SeqControlFlowStatement whileTrue =
      new SeqControlFlowStatement(
          SeqIntegerLiteralExpression.INT_1, SeqControlFlowStatementType.WHILE);

  private final CIdExpression numThreads;

  private final ImmutableList<SeqFunctionCallExpression> threadAssumptions;

  /** The thread-specific case clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses;

  private final ImmutableList<CVariableDeclaration> pcDeclarations;

  private final CFunctionCallAssignmentStatement assignNextThread;

  private final SeqFunctionCallExpression assumeNextThread;

  private final SeqStatement assumeNextThreadPc;

  // optional: POR variables
  private final Optional<ImmutableList<SeqFunctionCallExpression>> porAssumptions;

  private final Optional<CExpressionAssignmentStatement> assignPrevThread;

  // optional: sequentialization errors at loop head
  private final Optional<ImmutableList<SeqLogicalAndExpression>> loopInvariants;

  public SeqMainFunction(
      int pNumThreads,
      boolean pScalarPc,
      Optional<ImmutableList<SeqLogicalAndExpression>> pLoopInvariants,
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions,
      Optional<ImmutableList<SeqFunctionCallExpression>> pPORAssumptions,
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
    loopInvariants = pLoopInvariants;
    threadAssumptions = pThreadAssumptions;
    porAssumptions = pPORAssumptions;
    caseClauses = pCaseClauses;

    pcDeclarations = createPcDeclarations(pNumThreads, pScalarPc);

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
        new SeqFunctionCallExpression(SeqIdExpression.ASSUME, boundNextThreadExpression());
    assumeNextThreadPc = createNextThreadPcAssumption(pNumThreads, pScalarPc);

    assignPrevThread =
        porAssumptions.isPresent()
            ? Optional.of(
                new CExpressionAssignmentStatement(
                    FileLocation.DUMMY, SeqIdExpression.PREV_THREAD, SeqIdExpression.NEXT_THREAD))
            : Optional.empty();
  }

  private ImmutableList<CVariableDeclaration> createPcDeclarations(
      int pNumThreads, boolean pScalarPc) {

    // TODO once ACTIVE vars are removed, make sure to only init the main thread pc with 0,
    //  all other with -1
    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    if (pScalarPc) {
      // declare scalar int for each thread: pc0 = 0; pc1 = 0; ...
      for (int i = 0; i < pNumThreads; i++) {
        rDeclarations.add(
            SeqVariableDeclaration.buildVarDec(
                false,
                SeqSimpleType.INT,
                SeqExpressions.getPcExpression(i).toASTString(),
                SeqInitializer.INT_0));
      }
    } else {
      // declare int array declaration: pc[] = { 0, 0, ... };
      CInitializerList pcInitializerList =
          SeqInitializerList.buildIntInitializerList(
              SeqIntegerLiteralExpression.INT_0, pNumThreads);
      rDeclarations.add(
          SeqVariableDeclaration.buildVarDec(
              false, SeqArrayType.INT_ARRAY, SeqToken.pc, pcInitializerList));
    }
    return rDeclarations.build();
  }

  @Override
  public String toASTString() {
    String declarations = buildDeclarationsString(numThreads.getDeclaration(), pcDeclarations);
    String assertions = buildLoopInvariantsString(loopInvariants);
    String assumptions = buildAssumptionsString(threadAssumptions, porAssumptions);
    String switches = buildSwitchStatementsString(caseClauses);
    return getDeclarationWithParameterNames()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, declarations)
        + (assignPrevThread.isPresent()
            ? SeqUtil.prependTabsWithNewline(1, SeqVariableDeclaration.PREV_THREAD.toASTString())
            : SeqSyntax.EMPTY_STRING)
        + SeqUtil.prependTabsWithNewline(1, SeqVariableDeclaration.NEXT_THREAD.toASTString())
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, SeqUtil.appendOpeningCurly(whileTrue.toASTString()))
        + assertions
        + SeqUtil.prependTabsWithNewline(2, assignNextThread.toASTString())
        + SeqUtil.prependTabsWithNewline(2, assumeNextThread.toASTString() + SeqSyntax.SEMICOLON)
        + ((assumeNextThreadPc instanceof SeqSwitchStatement)
            ? assumeNextThreadPc.toASTString() + SeqSyntax.NEWLINE
            : SeqUtil.prependTabsWithNewline(2, assumeNextThreadPc.toASTString()))
        + SeqSyntax.NEWLINE
        + assumptions
        + (assignPrevThread.isPresent()
            ? SeqSyntax.NEWLINE
                + SeqUtil.prependTabsWithNewline(2, assignPrevThread.orElseThrow().toASTString())
                + SeqSyntax.NEWLINE
            : SeqSyntax.NEWLINE)
        + switches
        + SeqUtil.prependTabsWithNewline(2, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, SeqSyntax.CURLY_BRACKET_RIGHT)
        + SeqUtil.prependTabsWithNewline(1, Sequentialization.outputReachErrorDummy)
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

  private String buildDeclarationsString(
      CSimpleDeclaration pNumThreads, ImmutableList<CVariableDeclaration> pPcDeclarations) {

    StringBuilder rDeclarations = new StringBuilder();
    rDeclarations.append(pNumThreads.toASTString()).append(SeqSyntax.NEWLINE);
    for (CVariableDeclaration varDec : pPcDeclarations) {
      rDeclarations.append(SeqUtil.prependTabsWithNewline(1, varDec.toASTString()));
    }
    return rDeclarations.toString();
  }

  private String buildLoopInvariantsString(
      Optional<ImmutableList<SeqLogicalAndExpression>> pLoopInvariants) {

    StringBuilder rLoopInvariants = new StringBuilder();
    if (pLoopInvariants.isPresent()) {
      for (SeqLogicalAndExpression assertion : pLoopInvariants.orElseThrow()) {
        SeqControlFlowStatement ifStmt =
            new SeqControlFlowStatement(assertion, SeqControlFlowStatementType.IF);
        rLoopInvariants.append(
            SeqUtil.prependTabsWithNewline(2, SeqUtil.appendOpeningCurly(ifStmt.toASTString())));
        rLoopInvariants.append(
            SeqUtil.prependTabsWithNewline(
                3, SeqUtil.appendClosingCurly(Sequentialization.outputReachErrorDummy)));
      }
      rLoopInvariants.append(SeqSyntax.NEWLINE);
    }
    return rLoopInvariants.toString();
  }

  private String buildAssumptionsString(
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions,
      Optional<ImmutableList<SeqFunctionCallExpression>> pPORAssumptions) {

    StringBuilder rAssumptions = new StringBuilder();
    for (SeqFunctionCallExpression assumption : pThreadAssumptions) {
      String assumeStmt = assumption.toASTString() + SeqSyntax.SEMICOLON;
      rAssumptions.append(SeqUtil.prependTabsWithNewline(2, assumeStmt));
    }
    if (pPORAssumptions.isPresent()) {
      if (!pPORAssumptions.orElseThrow().isEmpty()) {
        rAssumptions.append(SeqSyntax.NEWLINE);
      }
      for (SeqFunctionCallExpression assumption : pPORAssumptions.orElseThrow()) {
        String assumeStmt = assumption.toASTString() + SeqSyntax.SEMICOLON;
        rAssumptions.append(SeqUtil.prependTabsWithNewline(2, assumeStmt));
      }
    }
    return rAssumptions.toString();
  }

  private String buildSwitchStatementsString(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    StringBuilder rSwitches = new StringBuilder();
    int i = 0;
    for (var entry : pCaseClauses.entrySet()) {
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
        rSwitches.append(
            SeqUtil.prependTabsWithoutNewline(
                2,
                i == 0
                    ? SeqUtil.appendOpeningCurly(stmt.toASTString())
                    : SeqUtil.wrapInCurlyOutwards(stmt.toASTString())));
      } catch (UnrecognizedCodeException e) {
        throw new RuntimeException(e);
      }
      rSwitches.append(SeqSyntax.NEWLINE);
      CExpression pcExpr = SeqExpressions.getPcExpression(thread.id);
      SeqSwitchStatement switchCaseExpr = new SeqSwitchStatement(pcExpr, entry.getValue(), 3);
      rSwitches.append(switchCaseExpr.toASTString());

      // append 2 newlines, except for last switch case (1 only)
      rSwitches.append(SeqUtil.repeat(SeqSyntax.NEWLINE, i == caseClauses.size() - 1 ? 1 : 2));
      i++;
    }
    return rSwitches.toString();
  }

  /** Returns the expression {@code 0 <= next_thread && next_thread < NUM_THREADS} */
  private ImmutableList<SeqExpression> boundNextThreadExpression()
      throws UnrecognizedCodeException {
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

  private SeqStatement createNextThreadPcAssumption(int pNumThreads, boolean pScalarPc)
      throws UnrecognizedCodeException {

    if (pScalarPc) {
      // scalar pc int: switch statement with individual case i: assume(pci != -1);
      ImmutableList.Builder<SeqCaseClause> assumeCaseClauses = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        SeqFunctionCallStatement assumeCall =
            new SeqFunctionCallStatement(
                new SeqFunctionCallExpression(
                    SeqIdExpression.ASSUME,
                    ImmutableList.of(
                        new CToSeqExpression(
                            SeqBinaryExpression.buildBinaryExpression(
                                SeqIdExpression.scalarPc().get(i),
                                SeqIntegerLiteralExpression.INT_EXIT_PC,
                                BinaryOperator.NOT_EQUALS)))));
        assumeCaseClauses.add(
            new SeqCaseClause(
                false,
                i,
                new SeqCaseBlock(
                    ImmutableList.of(new SeqScalarPcAssumeStatement(assumeCall)),
                    Terminator.BREAK)));
      }
      return new SeqSwitchStatement(SeqIdExpression.NEXT_THREAD, assumeCaseClauses.build(), 2);
    } else {
      // pc array: single assume(pc[next_thread] != -1);
      return new SeqFunctionCallStatement(
          new SeqFunctionCallExpression(
              SeqIdExpression.ASSUME,
              ImmutableList.of(
                  new CToSeqExpression(
                      SeqBinaryExpression.buildBinaryExpression(
                          SeqArraySubscriptExpression.buildPcSubscriptExpr(
                              SeqIdExpression.NEXT_THREAD),
                          SeqIntegerLiteralExpression.INT_EXIT_PC,
                          BinaryOperator.NOT_EQUALS)))));
    }
  }

  /** Returns the expression {@code pc[next_thread] != -1} */
  private ImmutableList<SeqExpression> assumeNextThreadActiveParams()
      throws UnrecognizedCodeException {
    ImmutableList.Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new CToSeqExpression(
            SeqBinaryExpression.buildBinaryExpression(
                SeqArraySubscriptExpression.buildPcSubscriptExpr(SeqIdExpression.NEXT_THREAD),
                SeqIntegerLiteralExpression.INT_EXIT_PC,
                BinaryOperator.NOT_EQUALS)));
    return rParams.build();
  }
}
