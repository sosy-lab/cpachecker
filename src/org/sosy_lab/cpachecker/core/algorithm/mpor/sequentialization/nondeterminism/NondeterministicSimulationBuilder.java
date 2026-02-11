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
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqThreadSimulationFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;

/**
 * Contains methods that can be used to build thread simulations based on the specified {@link
 * NondeterminismSource}.
 */
public class NondeterministicSimulationBuilder {

  public static NondeterministicSimulation buildNondeterministicSimulationBySource(
      MPOROptions pOptions,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    return switch (pOptions.nondeterminismSource()) {
      case NEXT_THREAD ->
          new NextThreadNondeterministicSimulation(
              pOptions, pMemoryModel, pGhostElements, pClauses, pUtils);
      case NEXT_THREAD_AND_NUM_STATEMENTS ->
          new NextThreadAndNumStatementsNondeterministicSimulation(
              pOptions, pMemoryModel, pGhostElements, pClauses, pUtils);
      case NUM_STATEMENTS ->
          new NumStatementsNondeterministicSimulation(
              pOptions, pMemoryModel, pGhostElements, pClauses, pUtils);
    };
  }

  // Thread Simulation Functions ===================================================================

  public static ImmutableList<SeqThreadSimulationFunction> buildThreadSimulationFunctions(
      MPOROptions pOptions,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadSimulationFunction> rFunctions = ImmutableList.builder();
    for (MPORThread thread : pClauses.keySet()) {
      CCompoundStatement threadSimulation =
          buildNondeterministicSimulationBySource(
                  pOptions, pMemoryModel, pGhostElements, pClauses, pUtils)
              .buildSingleThreadSimulation(thread);
      rFunctions.add(new SeqThreadSimulationFunction(pOptions, threadSimulation, thread));
    }
    return rFunctions.build();
  }

  public static ImmutableList<CFunctionCallStatement> buildThreadSimulationFunctionCallStatements(
      MPOROptions pOptions, ImmutableList<SeqThreadSimulationFunction> pThreadSimulationFunctions) {

    ImmutableList.Builder<CFunctionCallStatement> rFunctionCalls = ImmutableList.builder();
    // start with main thread function call
    SeqThreadSimulationFunction mainThreadFunction =
        Objects.requireNonNull(
            Iterables.getOnlyElement(
                pThreadSimulationFunctions.stream()
                    .filter(Objects::nonNull)
                    .filter(f -> f.thread.isMain())
                    .toList()));
    rFunctionCalls.add(mainThreadFunction.buildFunctionCallStatement(ImmutableList.of()));
    for (int i = 0; i < pOptions.loopIterations(); i++) {
      for (SeqThreadSimulationFunction function : pThreadSimulationFunctions) {
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

  // injections ====================================================================================

  static ImmutableList<SeqThreadStatementClause> injectStatementsIntoSingleThreadClauses(
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

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      SeqThreadStatement withRoundGoto =
          tryInjectRoundGotoIntoStatement(
              pOptions, statement, pLabelClauseMap, pBinaryExpressionBuilder);
      newStatements.add(withRoundGoto);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private static SeqThreadStatement tryInjectRoundGotoIntoStatement(
      MPOROptions pOptions,
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pStatement.data().targetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.data().targetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // check if the target is a separate loop
        if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, target)) {
          return injectRoundGotoIntoStatementByTargetPc(
              targetPc, pStatement, pLabelClauseMap, pBinaryExpressionBuilder);
        }
      }
    }
    if (pStatement.data().targetGoto().isPresent()) {
      // target goto present -> use goto label for injection
      return injectRoundGotoIntoStatementByTargetGoto(
          pStatement.data().targetGoto().orElseThrow(), pStatement, pBinaryExpressionBuilder);
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static SeqThreadStatement injectRoundGotoIntoStatementByTargetPc(
      int pTargetPc,
      SeqThreadStatement pStatement,
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
    SeqInstrumentation roundGoto =
        SeqInstrumentationBuilder.buildGuardedGotoStatement(
            roundSmallerMax,
            ImmutableList.of(roundIncrement),
            Objects.requireNonNull(target).getFirstBlock().getLabel().toCLabelStatement());
    return SeqThreadStatementUtil.appendedInstrumentationStatement(pStatement, roundGoto);
  }

  private static SeqThreadStatement injectRoundGotoIntoStatementByTargetGoto(
      SeqBlockLabelStatement pTargetGoto,
      SeqThreadStatement pStatement,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CBinaryExpression roundSmallerMax =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.ROUND, SeqIdExpressions.ROUND_MAX, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement roundIncrement =
        SeqStatementBuilder.buildIncrementStatement(
            SeqIdExpressions.ROUND, pBinaryExpressionBuilder);
    SeqInstrumentation roundGoto =
        SeqInstrumentationBuilder.buildGuardedGotoStatement(
            roundSmallerMax, ImmutableList.of(roundIncrement), pTargetGoto.toCLabelStatement());
    return SeqThreadStatementUtil.appendedInstrumentationStatement(pStatement, roundGoto);
  }

  // thread_count injections =======================================================================

  private static SeqThreadStatementBlock injectCountUpdatesIntoBlock(
      SeqThreadStatementBlock pBlock, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      // use a list for thread_count updates, since we can increment and decrement in one statement
      // if the statement creates another thread but also terminates the current thread.
      List<CExpressionAssignmentStatement> countUpdates = new ArrayList<>();
      // if a thread is created -> thread_count = thread_count + 1
      if (statement.data().type().equals(SeqThreadStatementType.THREAD_CREATION)) {
        countUpdates.add(
            SeqStatementBuilder.buildIncrementStatement(
                SeqIdExpressions.THREAD_COUNT, pBinaryExpressionBuilder));
      }
      // if a thread exits -> thread_count = thread_count - 1
      if (statement.isTargetPcExit()) {
        countUpdates.add(
            SeqStatementBuilder.buildDecrementStatement(
                SeqIdExpressions.THREAD_COUNT, pBinaryExpressionBuilder));
      }
      ImmutableList<SeqInstrumentation> guardedGotos =
          countUpdates.stream()
              .map(SeqInstrumentationBuilder::buildThreadCountUpdateStatement)
              .collect(ImmutableList.toImmutableList());
      newStatements.add(
          SeqThreadStatementUtil.appendedInstrumentationStatement(statement, guardedGotos));
    }
    return pBlock.withStatements(newStatements.build());
  }

  // single active thread injections ===============================================================

  private static SeqThreadStatementBlock injectSingleActiveThreadIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      if (statement.isTargetPcValid()) {
        int targetPc = statement.data().targetPc().orElseThrow();
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // check if the target is a separate loop
        if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, target)) {
          // thread_count == 1
          CBinaryExpression threadCountEqualsOne =
              pBinaryExpressionBuilder.buildBinaryExpression(
                  SeqIdExpressions.THREAD_COUNT,
                  SeqIntegerLiteralExpressions.INT_1,
                  BinaryOperator.EQUALS);
          SeqInstrumentation singleActiveThreadGoto =
              SeqInstrumentationBuilder.buildGuardedGotoStatement(
                  threadCountEqualsOne,
                  ImmutableList.of(),
                  Objects.requireNonNull(target).getFirstBlock().getLabel().toCLabelStatement());
          newStatements.add(
              SeqThreadStatementUtil.appendedInstrumentationStatement(
                  statement, singleActiveThreadGoto));
        } else {
          // if the target is a loop head + noBackwardLoopGoto is enabled -> no replacement
          newStatements.add(statement);
        }
      } else {
        // no valid target pc -> no replacement
        newStatements.add(statement);
      }
    }
    return pBlock.withStatements(newStatements.build());
  }

  // sync injections ===============================================================================

  private static SeqThreadStatementBlock injectSyncUpdatesIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncFlag,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (!pOptions.reduceIgnoreSleep()) {
      return pBlock;
    }
    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      SeqThreadStatement withGoto =
          tryInjectSyncUpdateIntoStatement(statement, pSyncFlag, pLabelClauseMap);
      newStatements.add(withGoto);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private static SeqThreadStatement tryInjectSyncUpdateIntoStatement(
      SeqThreadStatement pStatement,
      CIdExpression pSyncVariable,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.data().targetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.data().targetPc().orElseThrow();
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

  private static SeqThreadStatement injectSyncUpdateIntoStatementByTargetPc(
      SeqThreadStatement pStatement,
      Optional<SeqThreadStatementClause> pTargetClause,
      CIdExpression pSyncVariable) {

    boolean isSync =
        pTargetClause.isPresent()
            && SeqThreadStatementUtil.anySynchronizesThreads(
                pTargetClause.orElseThrow().getAllStatements());
    CIntegerLiteralExpression value =
        isSync ? SeqIntegerLiteralExpressions.INT_1 : SeqIntegerLiteralExpressions.INT_0;
    SeqInstrumentation syncUpdate =
        SeqInstrumentationBuilder.buildThreadSyncUpdateStatement(pSyncVariable, value);
    return SeqThreadStatementUtil.appendedInstrumentationStatement(pStatement, syncUpdate);
  }
}
