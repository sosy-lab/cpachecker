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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumption;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SeqBitVectorType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
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

  private final ImmutableList<CIdExpression> updatedVariables;

  private final CIdExpression numThreads;

  private final ImmutableListMultimap<MPORThread, SeqAssumption> threadAssumptions;

  /** The thread-specific case clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses;

  private final ImmutableList<SeqBitVectorDeclaration> bitVectorDeclarations;

  private final ImmutableList<CVariableDeclaration> pcDeclarations;

  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  private final SeqFunctionCallExpression nextThreadAssumption;

  private final SeqStatement pcNextThreadAssumption;

  private final PcVariables pcVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public SeqMainFunction(
      MPOROptions pOptions,
      ImmutableList<CIdExpression> pUpdatedVariables,
      int pNumThreads,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    options = pOptions;
    updatedVariables = pUpdatedVariables;
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

    bitVectorDeclarations =
        createBitVectorDeclarations(pBitVectorVariables, pGlobalVariableIds, pCaseClauses);
    pcDeclarations = createPcDeclarations(pNumThreads, pOptions.scalarPc);

    nextThreadAssignment = buildNextThreadAssignment(pOptions.signedNondet);
    nextThreadAssumption = buildNextThreadAssumption(pOptions.signedNondet);
    pcNextThreadAssumption =
        buildPcNextThreadAssumption(pNumThreads, pOptions.scalarPc, pcVariables);
  }

  private ImmutableList<SeqBitVectorDeclaration> createBitVectorDeclarations(
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    int binaryLength = BitVectorUtil.getBinaryLength(pGlobalVariableIds.size());
    SeqBitVectorType type = BitVectorUtil.getTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (var entry : pBitVectorVariables.bitVectors.entrySet()) {
      SeqCaseClause firstCase = Objects.requireNonNull(pCaseClauses.get(entry.getKey())).get(0);
      ImmutableSet<CVariableDeclaration> firstCaseGlobalVariables =
          SeqCaseClauseUtil.findAllGlobalVariablesInCaseClause(firstCase);
      SeqBitVector bitVector =
          BitVectorUtil.createBitVector(options, pGlobalVariableIds, firstCaseGlobalVariables);
      SeqBitVectorDeclaration declaration =
          new SeqBitVectorDeclaration(type, entry.getValue(), bitVector);
      rDeclarations.add(declaration);
    }
    return rDeclarations.build();
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
  public ImmutableList<LineOfCode> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rBody = ImmutableList.builder();
    // declare main() local variables NUM_THREADS, pc, next_thread
    // TODO its probably best to remove num threads entirely and just place the int
    rBody.addAll(
        buildVariableDeclarations(
            options, numThreads.getDeclaration(), bitVectorDeclarations, pcDeclarations));
    // add updated injected variables that were pruned in partial order reduction
    rBody.addAll(buildVariableUpdates(updatedVariables));
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
    // we only add the global assumptions if thread specific loops (and assumptions) are disabled
    if (!options.threadLoops) {
      // only add label if bit vectors are enabled, otherwise they are not used
      if (options.porBitVector && options.porConcat) {
        SeqThreadLoopLabelStatement labelStatement =
            new SeqThreadLoopLabelStatement(SeqToken.ASSUME);
        rBody.add(LineOfCode.of(2, labelStatement.toASTString()));
      }
      rBody.addAll(buildSingleLoopAssumptions(threadAssumptions));
    }
    // add all switch statements
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.THREAD_SIMULATION_SWITCHES));
    }
    if (options.threadLoops) {
      rBody.addAll(buildThreadLoopsSwitchStatements(options, threadAssumptions, caseClauses));
    } else {
      rBody.addAll(buildSingleLoopSwitchStatements(options, caseClauses));
    }
    rBody.add(LineOfCode.of(1, SeqSyntax.CURLY_BRACKET_RIGHT));
    // --- loop ends here ---
    if (options.sequentializationErrors) {
      // end of main function, only reachable if thread simulation finished incorrectly -> error
      rBody.add(LineOfCode.of(1, Sequentialization.outputReachErrorDummy));
    }
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

  private ImmutableList<LineOfCode> buildVariableUpdates(
      ImmutableList<CIdExpression> pUpdatedVariables) {

    ImmutableList.Builder<LineOfCode> rVariableUpdates = ImmutableList.builder();
    for (CIdExpression variable : pUpdatedVariables) {
      CExpressionAssignmentStatement assignment =
          SeqStatementBuilder.buildExpressionAssignmentStatement(
              variable, SeqIntegerLiteralExpression.INT_1);
      rVariableUpdates.add(LineOfCode.of(1, assignment.toASTString()));
    }
    return rVariableUpdates.build();
  }

  /** Returns {@link LineOfCode} for {@code NUM_THREADS} and {@code pc} declarations. */
  private ImmutableList<LineOfCode> buildVariableDeclarations(
      MPOROptions pOptions,
      CSimpleDeclaration pNumThreads,
      ImmutableList<SeqBitVectorDeclaration> pBitVectorDeclarations,
      ImmutableList<CVariableDeclaration> pPcDeclarations) {

    ImmutableList.Builder<LineOfCode> rVariableDeclarations = ImmutableList.builder();

    // NUM_THREADS
    rVariableDeclarations.add(LineOfCode.of(1, pNumThreads.toASTString()));

    // next_thread
    if (pOptions.signedNondet) {
      rVariableDeclarations.add(
          LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD_SIGNED.toASTString()));
    } else {
      rVariableDeclarations.add(
          LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD_UNSIGNED.toASTString()));
    }

    // pc
    if (pOptions.comments) {
      rVariableDeclarations.add(LineOfCode.empty());
      rVariableDeclarations.add(LineOfCode.of(1, SeqComment.PC_DECLARATION));
    }
    for (CVariableDeclaration pcDeclaration : pPcDeclarations) {
      rVariableDeclarations.add(LineOfCode.of(1, pcDeclaration.toASTString()));
    }

    // if enabled: bit vectors (for partial order reductions)
    if (pOptions.porBitVector) {
      for (SeqBitVectorDeclaration bitVectorDeclaration : pBitVectorDeclarations) {
        rVariableDeclarations.add(LineOfCode.of(1, bitVectorDeclaration.toASTString()));
      }
    }

    // if enabled: K and r (for thread loops iterations)
    if (pOptions.threadLoops) {
      rVariableDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.R.toASTString()));
      if (pOptions.signedNondet) {
        rVariableDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.K_SIGNED.toASTString()));
      } else {
        rVariableDeclarations.add(
            LineOfCode.of(1, SeqVariableDeclaration.K_UNSIGNED.toASTString()));
      }
    }

    return rVariableDeclarations.build();
  }

  private CFunctionCallAssignmentStatement buildNextThreadAssignment(boolean pIsSigned) {
    return new CFunctionCallAssignmentStatement(
        FileLocation.DUMMY,
        SeqIdExpression.NEXT_THREAD,
        pIsSigned
            ? SeqExpressionBuilder.buildVerifierNondetInt()
            : SeqExpressionBuilder.buildVerifierNondetUint());
  }

  private SeqFunctionCallExpression buildNextThreadAssumption(boolean pIsSigned)
      throws UnrecognizedCodeException {

    return new SeqFunctionCallExpression(
        SeqIdExpression.ASSUME, buildNextThreadAssumptionExpression(pIsSigned));
  }

  private ImmutableList<LineOfCode> buildSingleLoopAssumptions(
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions) {

    ImmutableList.Builder<LineOfCode> rAssumptions = ImmutableList.builder();
    for (SeqAssumption assumption : pThreadAssumptions.values()) {
      // for single loops, we use the entire OR expression
      SeqFunctionCallExpression assumeCall =
          SeqAssumptionBuilder.buildAssumeCall(assumption.toLogicalOrExpression());
      rAssumptions.add(LineOfCode.of(2, assumeCall.toASTString() + SeqSyntax.SEMICOLON));
    }
    return rAssumptions.build();
  }

  private ImmutableList<LineOfCode> buildThreadLoopAssumptions(
      ImmutableList<SeqAssumption> pAssumptions) {

    ImmutableList.Builder<LineOfCode> rAssumptions = ImmutableList.builder();
    for (SeqAssumption assumption : pAssumptions) {
      // we only add antecedents for thread loops
      SeqFunctionCallExpression assumeCall =
          SeqAssumptionBuilder.buildAssumeCall(assumption.antecedent);
      rAssumptions.add(LineOfCode.of(3, assumeCall.toASTString() + SeqSyntax.SEMICOLON));
    }
    return rAssumptions.build();
  }

  private ImmutableList<LineOfCode> buildThreadLoopsSwitchStatements(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();

    CFunctionCallAssignmentStatement kNondet =
        SeqStatementBuilder.buildFunctionCallAssignmentStatement(
            SeqIdExpression.K,
            pOptions.signedNondet
                ? SeqExpressionBuilder.buildVerifierNondetInt()
                : SeqExpressionBuilder.buildVerifierNondetUint());
    CBinaryExpression kGreaterZero =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.K, SeqIntegerLiteralExpression.INT_0, BinaryOperator.GREATER_THAN);
    CExpressionAssignmentStatement rReset =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpression.R, SeqIntegerLiteralExpression.INT_0);
    CExpressionAssignmentStatement rIncrement =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpression.R,
            binaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.R, SeqIntegerLiteralExpression.INT_1, BinaryOperator.PLUS));

    if (pOptions.threadLoopsNext) {
      rThreadLoops.addAll(
          buildThreadLoopsWithNextThread(
              pOptions,
              pThreadAssumptions,
              pCaseClauses,
              kNondet,
              kGreaterZero,
              rReset,
              rIncrement));
    } else {
      rThreadLoops.addAll(
          buildThreadLoopsWithoutNextThread(
              pOptions,
              pThreadAssumptions,
              pCaseClauses,
              kNondet,
              kGreaterZero,
              rReset,
              rIncrement));
    }

    return rThreadLoops.build();
  }

  private ImmutableList<LineOfCode> buildThreadLoopsWithNextThread(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CExpressionAssignmentStatement pRIncrement)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();

    SeqFunctionCallStatement assumeKGreaterZero =
        new SeqFunctionCallStatement(
            SeqAssumptionBuilder.buildAssumeCall(new CToSeqExpression(pKGreaterZero)));

    rThreadLoops.add(LineOfCode.of(2, pKNondet.toASTString()));
    rThreadLoops.add(LineOfCode.of(2, assumeKGreaterZero.toASTString()));
    rThreadLoops.add(LineOfCode.of(2, pRReset.toASTString()));

    int i = 0;
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CIntegerLiteralExpression threadId =
          SeqExpressionBuilder.buildIntegerLiteralExpression(thread.id);

      CBinaryExpression nextThreadEqualsThreadId =
          binaryExpressionBuilder.buildBinaryExpression(
              SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS);
      // first switch case: use "if", otherwise "else if"
      SeqControlFlowStatementType statementType =
          i == 0 ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
      SeqControlFlowStatement statement =
          new SeqControlFlowStatement(nextThreadEqualsThreadId, statementType);
      rThreadLoops.add(
          LineOfCode.of(
              2,
              i == 0
                  ? SeqStringUtil.appendOpeningCurly(statement.toASTString())
                  : SeqStringUtil.wrapInCurlyOutwards(statement.toASTString())));

      ImmutableList<SeqCaseClause> cases = entry.getValue();
      rThreadLoops.addAll(
          buildThreadLoop(pOptions, thread, pThreadAssumptions.get(thread), pRIncrement, cases));
      i++;
    }
    rThreadLoops.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));

    return rThreadLoops.build();
  }

  private ImmutableList<LineOfCode> buildThreadLoopsWithoutNextThread(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CExpressionAssignmentStatement pRIncrement)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqCaseClause> cases = entry.getValue();

      // choose nondet iterations and reset current iteration before each loop
      rThreadLoops.add(LineOfCode.of(2, pKNondet.toASTString()));
      rThreadLoops.add(LineOfCode.of(2, pRReset.toASTString()));

      // add condition if loop still active and K > 0
      CBinaryExpression pcUnequalExitPc =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pcVariables, thread.id, binaryExpressionBuilder);
      SeqLogicalAndExpression loopCondition =
          new SeqLogicalAndExpression(pcUnequalExitPc, pKGreaterZero);
      SeqControlFlowStatement ifStatement =
          new SeqControlFlowStatement(loopCondition, SeqControlFlowStatementType.IF);
      rThreadLoops.add(
          LineOfCode.of(2, SeqStringUtil.appendOpeningCurly(ifStatement.toASTString())));

      // add the thread loop statements (assumptions and switch)
      rThreadLoops.addAll(
          buildThreadLoop(pOptions, thread, pThreadAssumptions.get(thread), pRIncrement, cases));
      rThreadLoops.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));
    }
    return rThreadLoops.build();
  }

  private ImmutableList<LineOfCode> buildThreadLoop(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<SeqAssumption> pThreadAssumptions,
      CExpressionAssignmentStatement pRIncrement,
      ImmutableList<SeqCaseClause> pCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoop = ImmutableList.builder();

    // create assumption and switch labels
    // TODO maybe add per-variable labels so that even less assumes are evaluated?
    SeqThreadLoopLabelStatement assumeLabel =
        new SeqThreadLoopLabelStatement(
            SeqNameUtil.buildThreadAssumeLabelName(pOptions, pThread.id));
    SeqThreadLoopLabelStatement switchLabel =
        new SeqThreadLoopLabelStatement(
            SeqNameUtil.buildThreadSwitchLabelName(pOptions, pThread.id));

    ImmutableList<LineOfCode> switchStatement =
        buildThreadLoopSwitchStatement(
            pOptions, pThread, assumeLabel, switchLabel, pCaseClauses, 3);

    // add all lines of code: loop head, assumptions, iteration increment, switch statement
    rThreadLoop.add(LineOfCode.of(3, assumeLabel.toASTString()));
    rThreadLoop.addAll(buildThreadLoopAssumptions(pThreadAssumptions));
    rThreadLoop.add(LineOfCode.of(3, switchLabel.toASTString()));
    rThreadLoop.add(LineOfCode.of(3, pRIncrement.toASTString()));
    rThreadLoop.addAll(switchStatement);

    return rThreadLoop.build();
  }

  private ImmutableList<LineOfCode> buildSingleLoopSwitchStatements(
      MPOROptions pOptions, ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

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
      if (pOptions.porBitVector && pOptions.porConcat) {
        SeqThreadLoopLabelStatement switchLabel =
            new SeqThreadLoopLabelStatement(
                SeqNameUtil.buildThreadSwitchLabelName(pOptions, thread.id));
        rSwitches.add(LineOfCode.of(3, switchLabel.toASTString()));
      }
      rSwitches.addAll(buildSingleLoopSwitchStatement(pOptions, thread, entry.getValue(), 3));
      i++;
    }
    rSwitches.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));
    return rSwitches.build();
  }

  private ImmutableList<LineOfCode> buildSingleLoopSwitchStatement(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<SeqCaseClause> pCaseClauses,
      int pTabs) {

    CExpression pcExpression = pcVariables.get(pThread.id);
    SeqSwitchStatement switchStatement =
        new SeqSwitchStatement(pOptions, pcExpression, pCaseClauses, pTabs);
    return LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString());
  }

  private ImmutableList<LineOfCode> buildThreadLoopSwitchStatement(
      MPOROptions pOptions,
      MPORThread pThread,
      SeqThreadLoopLabelStatement pAssumeLabel,
      SeqThreadLoopLabelStatement pSwitchLabel,
      ImmutableList<SeqCaseClause> pCaseClauses,
      int pTabs)
      throws UnrecognizedCodeException {

    CExpression pcExpression = pcVariables.get(pThread.id);
    CBinaryExpression iterationSmallerMax =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.R, SeqIdExpression.K, BinaryOperator.LESS_THAN);
    ImmutableList.Builder<SeqCaseClause> pUpdatedCases = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        SeqCaseBlockStatement newStatement =
            SeqCaseClauseUtil.injectGotoThreadLoop(
                iterationSmallerMax, pAssumeLabel, pSwitchLabel, statement);
        newStatements.add(newStatement);
      }
      pUpdatedCases.add(caseClause.cloneWithBlock(new SeqCaseBlock(newStatements.build())));
    }
    SeqSwitchStatement switchStatement =
        new SeqSwitchStatement(pOptions, pcExpression, pUpdatedCases.build(), pTabs);
    return LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString());
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
                        SeqCaseBlockStatementBuilder.buildScalarPcAssumeStatement(assumeCall)))));
      }
      return new SeqSwitchStatement(
          options, SeqIdExpression.NEXT_THREAD, assumeCaseClauses.build(), 0);
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
