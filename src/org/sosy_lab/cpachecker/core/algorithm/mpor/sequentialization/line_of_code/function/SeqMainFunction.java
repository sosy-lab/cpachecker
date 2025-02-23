// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqArraySubscriptExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction extends SeqFunction {

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

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  // TODO best put pc expressions (array and scalar) here as they are local to main()

  public SeqMainFunction(
      int pNumThreads,
      MPOROptions pOptions,
      Optional<ImmutableList<SeqLogicalAndExpression>> pLoopInvariants,
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions,
      Optional<ImmutableList<SeqFunctionCallExpression>> pPORAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    numThreads =
        SeqIdExpression.buildIdExpression(
            SeqVariableDeclaration.buildVariableDeclaration(
                false,
                SeqSimpleType.CONST_INT,
                SeqToken.NUM_THREADS,
                SeqInitializer.buildInitializerExpression(
                    SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pNumThreads))));
    loopInvariants = pLoopInvariants;
    threadAssumptions = pThreadAssumptions;
    porAssumptions = pPORAssumptions;
    caseClauses = pCaseClauses;
    binaryExpressionBuilder = pBinaryExpressionBuilder;

    pcDeclarations = createPcDeclarations(pNumThreads, pOptions.scalarPc);

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
    assumeNextThreadPc = createNextThreadPcAssumption(pNumThreads, pOptions.scalarPc);

    assignPrevThread =
        porAssumptions.isPresent()
            ? Optional.of(
                new CExpressionAssignmentStatement(
                    FileLocation.DUMMY, SeqIdExpression.PREV_THREAD, SeqIdExpression.NEXT_THREAD))
            : Optional.empty();
  }

  private ImmutableList<CVariableDeclaration> createPcDeclarations(
      int pNumThreads, boolean pScalarPc) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    if (pScalarPc) {
      // declare scalar int for each thread: pc0 = 0; pc1 = -1; ...
      for (int i = 0; i < pNumThreads; i++) {
        rDeclarations.add(
            SeqVariableDeclaration.buildVariableDeclaration(
                false,
                SeqSimpleType.INT,
                SeqExpressions.getPcExpression(i).toASTString(),
                i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1));
      }
    } else {
      // declare int array: pc[] = { 0, -1, ... };
      ImmutableList.Builder<CInitializer> initializers = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        initializers.add(i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1);
      }
      CInitializerList initializerList =
          new CInitializerList(FileLocation.DUMMY, initializers.build());
      rDeclarations.add(
          SeqVariableDeclaration.buildVariableDeclaration(
              false, SeqArrayType.INT_ARRAY, SeqToken.pc, initializerList));
    }
    return rDeclarations.build();
  }

  @Override
  public ImmutableList<LineOfCode> buildBody() {
    ImmutableList.Builder<LineOfCode> rBody = ImmutableList.builder();
    // declare main() local variables NUM_THREADS, pc, next_thread and optionally prev_thread
    rBody.addAll(buildVarDeclarations(numThreads.getDeclaration(), pcDeclarations));
    if (assignPrevThread.isPresent()) {
      rBody.add(LineOfCode.of(1, SeqVariableDeclaration.PREV_THREAD.toASTString()));
    }
    rBody.add(LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD.toASTString()));
    rBody.add(LineOfCode.empty());
    // --- loop starts here ---
    rBody.add(LineOfCode.of(1, SeqStringUtil.appendOpeningCurly(whileTrue.toASTString())));
    // optional: add loop invariants at loop head
    rBody.addAll(buildLoopInvariants(loopInvariants));
    rBody.add(LineOfCode.of(2, assignNextThread.toASTString()));
    rBody.add(LineOfCode.of(2, assumeNextThread.toASTString() + SeqSyntax.SEMICOLON));
    // add assumption over pc depending on array vs. scalar pc
    if (assumeNextThreadPc instanceof SeqSwitchStatement) {
      rBody.addAll(LineOfCodeUtil.buildLinesOfCode(assumeNextThreadPc.toASTString()));
    } else {
      rBody.add(LineOfCode.of(2, assumeNextThreadPc.toASTString()));
    }
    rBody.add(LineOfCode.empty());
    // add all assumptions over thread variables
    rBody.addAll(buildAssumptions(threadAssumptions, porAssumptions));
    if (assignPrevThread.isPresent()) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, assignPrevThread.orElseThrow().toASTString()));
    }
    rBody.add(LineOfCode.empty());
    // add all switch statements
    rBody.addAll(buildSwitchStatements(caseClauses));
    rBody.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));
    rBody.add(LineOfCode.of(1, SeqSyntax.CURLY_BRACKET_RIGHT));
    // --- loop ends here ---
    // end of main function, only reachable if thread simulation finished incorrectly -> error
    rBody.add(LineOfCode.of(1, Sequentialization.outputReachErrorDummy));
    return rBody.build();
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

  private ImmutableList<LineOfCode> buildVarDeclarations(
      CSimpleDeclaration pNumThreads, ImmutableList<CVariableDeclaration> pPcDeclarations) {

    ImmutableList.Builder<LineOfCode> rVarDeclarations = ImmutableList.builder();
    rVarDeclarations.add(LineOfCode.of(1, pNumThreads.toASTString()));
    for (CVariableDeclaration varDeclaration : pPcDeclarations) {
      rVarDeclarations.add(LineOfCode.of(1, varDeclaration.toASTString()));
    }
    return rVarDeclarations.build();
  }

  private ImmutableList<LineOfCode> buildLoopInvariants(
      Optional<ImmutableList<SeqLogicalAndExpression>> pLoopInvariants) {

    ImmutableList.Builder<LineOfCode> rLoopInvariants = ImmutableList.builder();
    if (pLoopInvariants.isPresent()) {
      for (SeqLogicalAndExpression assertion : pLoopInvariants.orElseThrow()) {
        SeqControlFlowStatement ifStatement =
            new SeqControlFlowStatement(assertion, SeqControlFlowStatementType.IF);
        rLoopInvariants.add(
            LineOfCode.of(2, SeqStringUtil.appendOpeningCurly(ifStatement.toASTString())));
        rLoopInvariants.add(
            LineOfCode.of(
                3, SeqStringUtil.appendClosingCurly(Sequentialization.outputReachErrorDummy)));
      }
      rLoopInvariants.add(LineOfCode.empty());
    }
    return rLoopInvariants.build();
  }

  private ImmutableList<LineOfCode> buildAssumptions(
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions,
      Optional<ImmutableList<SeqFunctionCallExpression>> pPORAssumptions) {

    ImmutableList.Builder<LineOfCode> rAssumptions = ImmutableList.builder();
    for (SeqFunctionCallExpression assumption : pThreadAssumptions) {
      String assumeStatement = assumption.toASTString() + SeqSyntax.SEMICOLON;
      rAssumptions.add(LineOfCode.of(2, assumeStatement));
    }
    if (pPORAssumptions.isPresent()) {
      if (!pPORAssumptions.orElseThrow().isEmpty()) {
        rAssumptions.add(LineOfCode.empty());
      }
      for (SeqFunctionCallExpression assumption : pPORAssumptions.orElseThrow()) {
        String assumeStatement = assumption.toASTString() + SeqSyntax.SEMICOLON;
        rAssumptions.add(LineOfCode.of(2, assumeStatement));
      }
    }
    return rAssumptions.build();
  }

  private ImmutableList<LineOfCode> buildSwitchStatements(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableList.Builder<LineOfCode> rSwitches = ImmutableList.builder();
    int i = 0;
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CIntegerLiteralExpression threadId =
          SeqIntegerLiteralExpression.buildIntegerLiteralExpression(thread.id);
      try {
        CBinaryExpression nextThreadEqualsThreadId =
            binaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS);
        // first switch case: use "if", otherwise "else if"
        SeqControlFlowStatementType statementType =
            i == 0 ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
        SeqControlFlowStatement statement =
            new SeqControlFlowStatement(nextThreadEqualsThreadId, statementType);
        rSwitches.add(
            LineOfCode.of(
                2,
                i == 0
                    ? SeqStringUtil.appendOpeningCurly(statement.toASTString())
                    : SeqStringUtil.wrapInCurlyOutwards(statement.toASTString())));
      } catch (UnrecognizedCodeException e) {
        throw new RuntimeException(e);
      }
      CExpression pcExpr = SeqExpressions.getPcExpression(thread.id);
      SeqSwitchStatement switchStatement = new SeqSwitchStatement(pcExpr, entry.getValue(), 3);
      rSwitches.addAll(LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString()));

      // append additional newlines between switch cases
      if (i != caseClauses.size() - 1) {
        rSwitches.add(LineOfCode.empty());
      }
      i++;
    }
    return rSwitches.build();
  }

  /** Returns the expression {@code 0 <= next_thread && next_thread < NUM_THREADS} */
  private ImmutableList<SeqExpression> boundNextThreadExpression()
      throws UnrecognizedCodeException {
    ImmutableList.Builder<SeqExpression> rParams = ImmutableList.builder();
    rParams.add(
        new SeqLogicalAndExpression(
            binaryExpressionBuilder.buildBinaryExpression(
                SeqIntegerLiteralExpression.INT_0,
                SeqIdExpression.NEXT_THREAD,
                BinaryOperator.LESS_EQUAL),
            binaryExpressionBuilder.buildBinaryExpression(
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
                            binaryExpressionBuilder.buildBinaryExpression(
                                SeqIdExpression.scalarPc().get(i),
                                SeqIntegerLiteralExpression.INT_EXIT_PC,
                                BinaryOperator.NOT_EQUALS)))));
        assumeCaseClauses.add(
            new SeqCaseClause(
                false,
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
                      binaryExpressionBuilder.buildBinaryExpression(
                          SeqArraySubscriptExpression.buildPcSubscriptExpression(
                              SeqIdExpression.NEXT_THREAD),
                          SeqIntegerLiteralExpression.INT_EXIT_PC,
                          BinaryOperator.NOT_EQUALS)))));
    }
  }
}
