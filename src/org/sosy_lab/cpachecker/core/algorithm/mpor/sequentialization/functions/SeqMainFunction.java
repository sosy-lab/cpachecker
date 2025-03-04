// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction extends SeqFunction {

  private static final SeqControlFlowStatement whileTrue =
      new SeqControlFlowStatement(
          SeqIntegerLiteralExpression.INT_1, SeqControlFlowStatementType.WHILE);

  private final MPOROptions options;

  private final CIdExpression numThreads;

  private final ImmutableList<SeqFunctionCallExpression> threadAssumptions;

  /** The thread-specific case clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses;

  private final ImmutableList<CVariableDeclaration> pcDeclarations;

  private final CVariableDeclaration nextThreadDeclaration;

  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  private final SeqFunctionCallExpression nextThreadAssumption;

  private final SeqStatement pcNextThreadAssumption;

  private final PcVariables pcVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqMainFunction(
      MPOROptions pOptions,
      int pNumThreads,
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    options = pOptions;
    numThreads =
        SeqExpressionBuilder.buildIdExpression(
            SeqDeclarationBuilder.buildVariableDeclaration(
                false,
                SeqSimpleType.CONST_INT,
                SeqToken.NUM_THREADS,
                SeqInitializerBuilder.buildInitializerExpression(
                    SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads))));
    threadAssumptions = pThreadAssumptions;
    caseClauses = pCaseClauses;
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;

    pcDeclarations = createPcDeclarations(pNumThreads, pOptions.scalarPc);

    nextThreadDeclaration =
        pOptions.signedNextThread
            ? SeqVariableDeclaration.NEXT_THREAD_SIGNED
            : SeqVariableDeclaration.NEXT_THREAD_UNSIGNED;
    nextThreadAssignment = buildNextThreadAssignment(pOptions.signedNextThread);
    nextThreadAssumption = buildNextThreadAssumption(pOptions.signedNextThread);
    pcNextThreadAssumption =
        buildPcNextThreadAssumption(pNumThreads, pOptions.scalarPc, pcVariables);
  }

  private ImmutableList<CVariableDeclaration> createPcDeclarations(
      int pNumThreads, boolean pScalarPc) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    if (pScalarPc) {
      // declare scalar int for each thread: pc0 = 0; pc1 = -1; ...
      for (int i = 0; i < pNumThreads; i++) {
        rDeclarations.add(
            SeqDeclarationBuilder.buildVariableDeclaration(
                false,
                SeqSimpleType.INT,
                pcVariables.get(i).toASTString(),
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
          SeqDeclarationBuilder.buildVariableDeclaration(
              false, SeqArrayType.INT_ARRAY, SeqToken.pc, initializerList));
    }
    return rDeclarations.build();
  }

  @Override
  public ImmutableList<LineOfCode> buildBody() {
    ImmutableList.Builder<LineOfCode> rBody = ImmutableList.builder();
    // declare main() local variables NUM_THREADS, pc, next_thread
    rBody.addAll(buildVariableDeclarations(numThreads.getDeclaration(), pcDeclarations));
    rBody.add(LineOfCode.of(1, nextThreadDeclaration.toASTString()));
    // --- loop starts here ---
    rBody.add(LineOfCode.of(1, SeqStringUtil.appendOpeningCurly(whileTrue.toASTString())));
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.NEXT_THREAD_NONDET));
    }
    rBody.add(LineOfCode.of(2, nextThreadAssignment.toASTString()));
    if (options.comments) {
      rBody.add(LineOfCode.empty());
    }
    rBody.add(LineOfCode.of(2, nextThreadAssumption.toASTString() + SeqSyntax.SEMICOLON));
    // assumptions over next_thread being active (pc != -1)
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.NEXT_THREAD_ACTIVE));
    }
    rBody.addAll(LineOfCodeUtil.buildLinesOfCode(2, pcNextThreadAssumption.toASTString()));
    // add all assumptions over thread variables
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.THREAD_SIMULATION_ASSUMPTIONS));
    }
    rBody.addAll(buildAssumptions(threadAssumptions));
    // add all switch statements
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.THREAD_SIMULATION_SWITCHES));
    }
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

  /** Returns {@link LineOfCode} for {@code NUM_THREADS} and {@code pc} declarations. */
  private ImmutableList<LineOfCode> buildVariableDeclarations(
      CSimpleDeclaration pNumThreads, ImmutableList<CVariableDeclaration> pPcDeclarations) {

    ImmutableList.Builder<LineOfCode> rVariableDeclarations = ImmutableList.builder();
    rVariableDeclarations.add(LineOfCode.of(1, pNumThreads.toASTString()));
    if (options.comments) {
      rVariableDeclarations.add(LineOfCode.empty());
      rVariableDeclarations.add(LineOfCode.of(1, SeqComment.PC_DECLARATION));
    }
    for (CVariableDeclaration varDeclaration : pPcDeclarations) {
      rVariableDeclarations.add(LineOfCode.of(1, varDeclaration.toASTString()));
    }
    return rVariableDeclarations.build();
  }

  private CFunctionCallAssignmentStatement buildNextThreadAssignment(boolean pIsSigned) {
    return new CFunctionCallAssignmentStatement(
        FileLocation.DUMMY,
        SeqIdExpression.NEXT_THREAD,
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            pIsSigned ? SeqSimpleType.INT : SeqSimpleType.UNSIGNED_INT,
            pIsSigned ? SeqIdExpression.VERIFIER_NONDET_INT : SeqIdExpression.VERIFIER_NONDET_UINT,
            ImmutableList.of(),
            pIsSigned
                ? SeqFunctionDeclaration.VERIFIER_NONDET_INT
                : SeqFunctionDeclaration.VERIFIER_NONDET_UINT));
  }

  private SeqFunctionCallExpression buildNextThreadAssumption(boolean pIsSigned)
      throws UnrecognizedCodeException {

    return new SeqFunctionCallExpression(
        SeqIdExpression.ASSUME, buildNextThreadAssumptionExpression(pIsSigned));
  }

  private ImmutableList<LineOfCode> buildAssumptions(
      ImmutableList<SeqFunctionCallExpression> pThreadAssumptions) {

    ImmutableList.Builder<LineOfCode> rAssumptions = ImmutableList.builder();
    for (SeqFunctionCallExpression assumption : pThreadAssumptions) {
      String assumeStatement = assumption.toASTString() + SeqSyntax.SEMICOLON;
      rAssumptions.add(LineOfCode.of(2, assumeStatement));
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
          SeqExpressionBuilder.buildIntegerLiteralExpression(thread.id);
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
      CExpression pcExpr = pcVariables.get(thread.id);
      SeqSwitchStatement switchStatement = new SeqSwitchStatement(pcExpr, entry.getValue(), 3);
      rSwitches.addAll(LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString()));
      i++;
    }
    return rSwitches.build();
  }

  /** Returns the expression {@code 0 <= next_thread && next_thread < NUM_THREADS} */
  private ImmutableList<SeqExpression> buildNextThreadAssumptionExpression(boolean pIsSigned)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    // next_thread < NUM_THREADS is used for both signed and unsigned
    CBinaryExpression nextThreadLessThanNumThreads =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.NEXT_THREAD, numThreads, BinaryOperator.LESS_THAN);
    rParameters.add(
        pIsSigned
            ? new SeqLogicalAndExpression(
                binaryExpressionBuilder.buildBinaryExpression(
                    SeqIntegerLiteralExpression.INT_0,
                    SeqIdExpression.NEXT_THREAD,
                    BinaryOperator.LESS_EQUAL),
                nextThreadLessThanNumThreads)
            : new CToSeqExpression(nextThreadLessThanNumThreads));
    return rParameters.build();
  }

  private SeqStatement buildPcNextThreadAssumption(
      int pNumThreads, boolean pScalarPc, PcVariables pPcVariables)
      throws UnrecognizedCodeException {

    if (pScalarPc) {
      // scalar pc int: switch statement with individual case i: assume(pci != -1);
      ImmutableList.Builder<SeqCaseClause> assumeCaseClauses = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        // ensure pc
        Verify.verify(pPcVariables.get(i) instanceof CIdExpression);
        SeqFunctionCallStatement assumeCall =
            new SeqFunctionCallStatement(
                new SeqFunctionCallExpression(
                    SeqIdExpression.ASSUME,
                    ImmutableList.of(
                        new CToSeqExpression(
                            binaryExpressionBuilder.buildBinaryExpression(
                                pPcVariables.get(i),
                                SeqIntegerLiteralExpression.INT_EXIT_PC,
                                BinaryOperator.NOT_EQUALS)))));
        assumeCaseClauses.add(
            new SeqCaseClause(
                false,
                false,
                i,
                new SeqCaseBlock(
                    ImmutableList.of(
                        SeqCaseBlockStatementBuilder.buildScalarPcAssumeStatement(assumeCall)),
                    Terminator.BREAK)));
      }
      return new SeqSwitchStatement(SeqIdExpression.NEXT_THREAD, assumeCaseClauses.build(), 0);
    } else {
      // pc array: single assume(pc[next_thread] != -1);
      return new SeqFunctionCallStatement(
          new SeqFunctionCallExpression(
              SeqIdExpression.ASSUME,
              ImmutableList.of(
                  new CToSeqExpression(
                      binaryExpressionBuilder.buildBinaryExpression(
                          SeqExpressionBuilder.buildPcSubscriptExpression(
                              SeqIdExpression.NEXT_THREAD),
                          SeqIntegerLiteralExpression.INT_EXIT_PC,
                          BinaryOperator.NOT_EQUALS)))));
    }
  }
}
