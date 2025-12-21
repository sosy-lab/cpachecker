// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqRoundGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqSingleActiveThreadGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqSyncUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqThreadSimulationFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Contains methods that can be used to build thread simulations based on the specified {@link
 * NondeterminismSource}.
 */
public class NondeterministicSimulationBuilder {

  public static String buildThreadSimulationsByNondeterminismSource(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.nondeterminismSource()) {
      case NEXT_THREAD, NEXT_THREAD_AND_NUM_STATEMENTS ->
          new NextThreadNondeterministicSimulation(
                  pOptions,
                  pFields.clauses,
                  pFields.ghostElements,
                  pUtils.binaryExpressionBuilder())
              .buildThreadSimulations();
      case NUM_STATEMENTS ->
          new NumStatementsNondeterministicSimulation(
                  pOptions, pFields.clauses, pFields.ghostElements, pUtils)
              .buildThreadSimulations();
    };
  }

  private static String buildSingleThreadSimulationByNondeterminismSource(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.nondeterminismSource()) {
      case NEXT_THREAD, NEXT_THREAD_AND_NUM_STATEMENTS ->
          buildSingleThreadSimulation(
                  pOptions,
                  pGhostElements,
                  pThread,
                  pClauses.get(pThread),
                  pUtils.binaryExpressionBuilder())
              .toASTString();
      case NUM_STATEMENTS ->
          new NumStatementsNondeterministicSimulation(pOptions, pClauses, pGhostElements, pUtils)
              .buildSingleThreadSimulation(pThread, pOtherThreads);
    };
  }

  // Thread Simulation Functions ===================================================================

  public static ImmutableList<SeqThreadSimulationFunction> buildThreadSimulationFunctions(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (!pOptions.loopUnrolling()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<SeqThreadSimulationFunction> rFunctions = ImmutableList.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(pClauses.keySet(), thread);
      String threadSimulation =
          buildSingleThreadSimulationByNondeterminismSource(
              pOptions, pGhostElements, thread, otherThreads, pClauses, pUtils);
      rFunctions.add(new SeqThreadSimulationFunction(pOptions, threadSimulation, thread));
    }
    return rFunctions.build();
  }

  public static ImmutableList<CFunctionCallStatement> buildThreadSimulationFunctionCallStatements(
      MPOROptions pOptions, SequentializationFields pFields) {

    ImmutableList.Builder<CFunctionCallStatement> rFunctionCalls = ImmutableList.builder();
    // start with main thread function call
    SeqThreadSimulationFunction mainThreadFunction =
        Objects.requireNonNull(
            Iterables.getOnlyElement(
                pFields.threadSimulationFunctions.stream()
                    .filter(Objects::nonNull)
                    .filter(f -> f.thread.isMain())
                    .toList()));
    rFunctionCalls.add(mainThreadFunction.buildFunctionCallStatement(ImmutableList.of()));
    for (int i = 0; i < pOptions.loopIterations(); i++) {
      for (SeqThreadSimulationFunction function : pFields.threadSimulationFunctions) {
        if (!function.thread.isMain()) {
          // continue with all other threads
          rFunctionCalls.add(function.buildFunctionCallStatement(ImmutableList.of()));
        }
      }
      // end on main thread
      rFunctionCalls.add(mainThreadFunction.buildFunctionCallStatement(ImmutableList.of()));
    }
    return rFunctionCalls.build();
  }

  // Multi Control Flow Statements =================================================================

  static SeqMultiControlStatement buildSingleThreadSimulation(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ProgramCounterVariables pcVariables = pGhostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.id());
    ImmutableList<CStatement> precedingStatements =
        buildPrecedingStatementsByNondeterminismSource(
            pOptions, pGhostElements, pThread, pBinaryExpressionBuilder);

    ImmutableList<SeqThreadStatementClause> withInjectedStatements =
        injectStatementsIntoSingleThreadClauses(
            pOptions,
            pGhostElements.threadSyncFlags().getSyncFlag(pThread),
            pClauses,
            pBinaryExpressionBuilder);
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pcVariables.getPcLeftHandSide(pThread.id()),
            withInjectedStatements,
            pBinaryExpressionBuilder);

    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        pOptions.controlEncodingStatement(),
        expression,
        precedingStatements,
        expressionClauseMap,
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<CStatement> buildPrecedingStatementsByNondeterminismSource(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pActiveThread,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.nondeterminismSource()) {
      case NEXT_THREAD, NEXT_THREAD_AND_NUM_STATEMENTS ->
          new NextThreadNondeterministicSimulation(
                  pOptions, ImmutableListMultimap.of(), pGhostElements, pBinaryExpressionBuilder)
              .buildPrecedingStatements(pActiveThread);
      case NUM_STATEMENTS -> {
        CExpressionAssignmentStatement roundReset = buildRoundReset();
        yield MultiControlStatementBuilder.buildPrecedingStatements(
            // assume("pc active") is not necessary since the simulation starts with 'if (pc* != 0)'
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(roundReset));
      }
    };
  }

  private static ImmutableList<SeqThreadStatementClause> injectStatementsIntoSingleThreadClauses(
      MPOROptions pOptions,
      CIdExpression pSyncFlag,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(
            injectCountAndRoundGotoIntoBlock(
                pOptions, block, pSyncFlag, labelClauseMap, pBinaryExpressionBuilder));
      }
      updatedClauses.add(clause.withBlocks(newBlocks.build()));
    }
    return updatedClauses.build();
  }

  // injections ====================================================================================

  private static SeqThreadStatementBlock injectCountAndRoundGotoIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncFlag,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqThreadStatementBlock updatedBlock =
        injectCountUpdatesIntoBlock(pBlock, pBinaryExpressionBuilder);
    if (pOptions.reduceSingleActiveThread()) {
      updatedBlock =
          injectSingleActiveThreadIntoBlock(
              pOptions, updatedBlock, pLabelClauseMap, pBinaryExpressionBuilder);
    }
    if (pOptions.nondeterminismSource().isNumStatementsNondeterministic()) {
      updatedBlock =
          injectRoundGotoIntoBlock(
              pOptions, updatedBlock, pLabelClauseMap, pBinaryExpressionBuilder);
    }
    return injectSyncUpdatesIntoBlock(pOptions, updatedBlock, pSyncFlag, pLabelClauseMap);
  }

  // round and round_max injections ================================================================

  /** Returns the expression for {@code round = 1;} */
  static CExpressionAssignmentStatement buildRoundReset() {
    // r is set to 1, because we increment after the r < K check succeeds
    return SeqStatementBuilder.buildExpressionAssignmentStatement(
        SeqIdExpressions.ROUND, SeqIntegerLiteralExpressions.INT_1);
  }

  private static SeqThreadStatementBlock injectRoundGotoIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      CSeqThreadStatement withRoundGoto =
          tryInjectRoundGotoIntoStatement(
              pOptions, statement, pLabelClauseMap, pBinaryExpressionBuilder);
      newStatements.add(withRoundGoto);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private static CSeqThreadStatement tryInjectRoundGotoIntoStatement(
      MPOROptions pOptions,
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // check if the target is a separate loop
        if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, target)) {
          return injectRoundGotoIntoStatementByTargetPc(
              targetPc, pStatement, pLabelClauseMap, pBinaryExpressionBuilder);
        }
      }
    }
    if (pStatement.getTargetGoto().isPresent()) {
      // target goto present -> use goto label for injection
      return injectRoundGotoIntoStatementByTargetGoto(
          pStatement.getTargetGoto().orElseThrow(), pStatement, pBinaryExpressionBuilder);
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static CSeqThreadStatement injectRoundGotoIntoStatementByTargetPc(
      int pTargetPc,
      CSeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(pTargetPc));
    CBinaryExpression roundSmallerMax =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.ROUND, SeqIdExpressions.ROUND_MAX, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement roundIncrement =
        SeqStatementBuilder.buildIncrementStatement(
            SeqIdExpressions.ROUND, pBinaryExpressionBuilder);
    SeqRoundGotoStatement roundGoto =
        new SeqRoundGotoStatement(
            roundSmallerMax,
            roundIncrement,
            Objects.requireNonNull(target).getFirstBlock().getLabel());
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(pStatement, roundGoto);
  }

  private static CSeqThreadStatement injectRoundGotoIntoStatementByTargetGoto(
      SeqBlockLabelStatement pTargetGoto,
      CSeqThreadStatement pStatement,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CBinaryExpression roundSmallerMax =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.ROUND, SeqIdExpressions.ROUND_MAX, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement roundIncrement =
        SeqStatementBuilder.buildIncrementStatement(
            SeqIdExpressions.ROUND, pBinaryExpressionBuilder);
    SeqRoundGotoStatement roundGoto =
        new SeqRoundGotoStatement(roundSmallerMax, roundIncrement, pTargetGoto);
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(pStatement, roundGoto);
  }

  // thread_count injections =======================================================================

  private static SeqThreadStatementBlock injectCountUpdatesIntoBlock(
      SeqThreadStatementBlock pBlock, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      CSeqThreadStatement withCountUpdates =
          tryInjectCountUpdatesIntoStatement(statement, pBinaryExpressionBuilder);
      newStatements.add(withCountUpdates);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private static CSeqThreadStatement tryInjectCountUpdatesIntoStatement(
      CSeqThreadStatement pStatement, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pStatement instanceof SeqThreadCreationStatement) {
      CExpressionAssignmentStatement countIncrement =
          SeqStatementBuilder.buildIncrementStatement(
              SeqIdExpressions.THREAD_COUNT, pBinaryExpressionBuilder);
      SeqCountUpdateStatement countIncrementStatement = new SeqCountUpdateStatement(countIncrement);
      return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
          pStatement, countIncrementStatement);

    } else if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
        CExpressionAssignmentStatement countDecrement =
            SeqStatementBuilder.buildDecrementStatement(
                SeqIdExpressions.THREAD_COUNT, pBinaryExpressionBuilder);
        SeqCountUpdateStatement countDecrementStatement =
            new SeqCountUpdateStatement(countDecrement);
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, countDecrementStatement);
      }
    }
    return pStatement;
  }

  // single active thread injections ===============================================================

  private static SeqThreadStatementBlock injectSingleActiveThreadIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      CSeqThreadStatement withSingleActiveThreadGoto =
          tryInjectSingleActiveThreadGotoIntoStatement(
              pOptions, statement, pLabelClauseMap, pBinaryExpressionBuilder);
      newStatements.add(withSingleActiveThreadGoto);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private static CSeqThreadStatement tryInjectSingleActiveThreadGotoIntoStatement(
      MPOROptions pOptions,
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // check if the target is a separate loop
        if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, target)) {
          // thread_count == 1
          CBinaryExpression threadCountEqualsOne =
              pBinaryExpressionBuilder.buildBinaryExpression(
                  SeqIdExpressions.THREAD_COUNT,
                  SeqIntegerLiteralExpressions.INT_1,
                  BinaryOperator.EQUALS);
          SeqSingleActiveThreadGotoStatement singleActiveThreadGoto =
              new SeqSingleActiveThreadGotoStatement(
                  threadCountEqualsOne, Objects.requireNonNull(target).getFirstBlock().getLabel());
          return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
              pStatement, singleActiveThreadGoto);
        }
      }
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  // sync injections ===============================================================================

  private static SeqThreadStatementBlock injectSyncUpdatesIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncFlag,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    // sync variables are only required with reduceIgnoreSleep enabled
    if (!pOptions.reduceIgnoreSleep()) {
      return pBlock;
    }
    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      CSeqThreadStatement withGoto =
          tryInjectSyncUpdateIntoStatement(statement, pSyncFlag, pLabelClauseMap);
      newStatements.add(withGoto);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private static CSeqThreadStatement tryInjectSyncUpdateIntoStatement(
      CSeqThreadStatement pStatement,
      CIdExpression pSyncVariable,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause targetClause =
            Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        return injectSyncUpdateIntoStatementByTargetPc(
            pStatement, Optional.of(targetClause), pSyncVariable);
      } else {
        return injectSyncUpdateIntoStatementByTargetPc(pStatement, Optional.empty(), pSyncVariable);
      }
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static CSeqThreadStatement injectSyncUpdateIntoStatementByTargetPc(
      CSeqThreadStatement pStatement,
      Optional<SeqThreadStatementClause> pTargetClause,
      CIdExpression pSyncVariable) {

    boolean isSync =
        pTargetClause.isPresent()
            && SeqThreadStatementUtil.anySynchronizesThreads(
                pTargetClause.orElseThrow().getAllStatements());
    CIntegerLiteralExpression value =
        isSync ? SeqIntegerLiteralExpressions.INT_1 : SeqIntegerLiteralExpressions.INT_0;
    SeqSyncUpdateStatement syncUpdate =
        new SeqSyncUpdateStatement(
            SeqStatementBuilder.buildExpressionAssignmentStatement(pSyncVariable, value));
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(pStatement, syncUpdate);
  }
}
