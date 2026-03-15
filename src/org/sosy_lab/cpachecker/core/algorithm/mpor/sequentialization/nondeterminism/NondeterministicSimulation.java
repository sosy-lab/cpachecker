// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.MultiSelectionStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.ReduceIgnoreSleepInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.ReduceLastThreadOrderInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CMultiSelectionStatementBuilder;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CSwitchStatement;

/**
 * Base class for simulating nondeterministic thread execution in the sequentialized program,
 * including methods to construct the multi-control statements, preceding statements, and full
 * simulation code strings.
 *
 * <p>Although similarly named, there is an important distinction between the abstract methods
 * {@code buildSingleThreadSimulation} and {@code buildAllThreadSimulations} based on whether the
 * option {@link MPOROptions#loopUnrolling()} is enabled (see examples below):
 *
 * <ul>
 *   <li>{@link #buildSingleThreadSimulation(MPORThread)} generates the code for a single thread
 *       simulation that can be placed in a separate function.
 *   <li>{@link #buildAllThreadSimulations()} generates the code for all thread simulations in a
 *       single block placed in {@code main()}.
 * </ul>
 *
 * <p>Example with {@link MPOROptions#loopUnrolling()} set to {@code false}:
 *
 * <pre>{@code
 * main() {
 *   while (1) {
 *     // String created by buildAllThreadSimulations():
 *     next_thread = nondet();
 *     switch (next_thread) {
 *       case 1: ...
 *       case ...
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Example with {@link MPOROptions#loopUnrolling()} set to {@code true}:
 *
 * <pre>{@code
 * T1() {
 *   // String created by buildSingleThreadSimulation():
 *   next_thread = nondet();
 *   if (next_thread == 1) { ... }
 * }
 * ... // repeat for all other threads
 * }</pre>
 *
 * <p>As shown, the code generated for {@code main()} cannot be reused for single thread functions,
 * since the control flow for the next-thread selection differs.
 */
public class NondeterministicSimulation {

  final MPOROptions options;

  final MachineModel machineModel;

  final Optional<MemoryModel> memoryModel;

  final ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses;

  final GhostElements ghostElements;

  final SequentializationUtils utils;

  public NondeterministicSimulation(
      MPOROptions pOptions,
      MachineModel pMachineModel,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    options = pOptions;
    machineModel = pMachineModel;
    memoryModel = pMemoryModel;
    ghostElements = pGhostElements;
    clauses = pClauses;
    utils = pUtils;
  }

  /**
   * Builds the {@link CCompoundStatement} code of a single simulation for the given {@code
   * pThread}, including the {@link MultiSelectionStatementEncoding} statement.
   *
   * <p>The resulting {@link CCompoundStatement} makes it possible for the simulation to be placed
   * in a separate function that can be called without any additional wrappers or preceding
   * statements. This is needed when {@link MPOROptions#loopUnrolling()} is enabled.
   */
  CCompoundStatement buildSingleThreadSimulation(MPORThread pThread)
      throws UnrecognizedCodeException {

    Builder<CCompoundStatementElement> rSimulation = ImmutableList.builder();

    // add "T{thread_id}: label", if present
    Optional<CLabelStatement> threadLabel =
        Optional.ofNullable(ghostElements.threadLabels().get(pThread));
    if (threadLabel.isPresent()) {
      rSimulation.add(threadLabel.orElseThrow());
    }

    // add "if (pc != 0 ...)" condition
    CBinaryExpression ifCondition =
        ghostElements.getPcVariables().getThreadActiveExpression(pThread.id());
    Builder<CCompoundStatementElement> ifBlock = ImmutableList.builder();

    // add the "{round_max, next_thread} = nondet;" assignments for this thread
    ifBlock.addAll(buildNondeterministicAssignments());

    // add the ignore sleep instrumentation, if enabled
    if (options.reduceIgnoreSleep()) {
      ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(clauses.keySet(), pThread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses.get(pThread));
      ReduceIgnoreSleepInjector reduceIgnoreSleepInjector =
          new ReduceIgnoreSleepInjector(
              options, pThread, otherThreads, labelClauseMap, ghostElements, utils);
      ifBlock.add(reduceIgnoreSleepInjector.buildIgnoreSleepInstrumentation());
    }

    // if ({round_max > 0, next_thread == i}) ...
    Builder<CCompoundStatementElement> innerIfBlock = ImmutableList.builder();
    CExportExpression nondeterministicIfCondition = buildNondeterministicIfCondition(pThread);

    // add the thread simulation statements
    innerIfBlock.addAll(buildAllPrecedingStatements(pThread));
    innerIfBlock.add(buildSingleThreadMultiSelectionStatement(pThread));
    CIfStatement innerIfStatement =
        new CIfStatement(nondeterministicIfCondition, new CCompoundStatement(innerIfBlock.build()));

    ifBlock.add(innerIfStatement);
    CIfStatement ifStatement =
        new CIfStatement(
            new CExpressionWrapper(ifCondition), new CCompoundStatement(ifBlock.build()));

    return new CCompoundStatement(rSimulation.add(ifStatement).build());
  }

  /**
   * Builds the {@link CCompoundStatement} code of all thread simulations, including wrapper
   * statements such as {@code if} guards. This is used only when {@link
   * MPOROptions#loopUnrolling()} is disabled, since then all thread simulations are placed as one
   * code block in the {@code main()} function.
   */
  public CCompoundStatement buildAllThreadSimulations() throws UnrecognizedCodeException {
    Builder<CCompoundStatementElement> rThreadSimulations = ImmutableList.builder();
    for (MPORThread thread : clauses.keySet()) {
      rThreadSimulations.add(buildSingleThreadSimulation(thread));
    }
    return new CCompoundStatement(rThreadSimulations.build());
  }

  // Preceding Statements

  private ImmutableList<CExportStatement> buildAllPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CExportStatement> allPreceding = ImmutableList.builder();

    if (options.reduceLastThreadOrder()) {
      // do not create the statement for the main thread, since LAST_THREAD < 0 never holds
      if (!pThread.isMain()) {
        ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
            SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses.get(pThread));
        ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
            SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses.get(pThread));
        CIfStatement lastThreadOrderStatement =
            new ReduceLastThreadOrderInjector(
                    options,
                    clauses.size(),
                    pThread,
                    labelClauseMap,
                    labelBlockMap,
                    ghostElements.bitVectorVariables().orElseThrow(),
                    machineModel,
                    memoryModel.orElseThrow(),
                    utils)
                .buildLastThreadOrderStatement(pThread);
        allPreceding.add(lastThreadOrderStatement);
      }
    }
    if (options.nondeterminismSource().isNumStatementsNondeterministic()) {
      allPreceding.add(new CStatementWrapper(NondeterministicSimulationBuilder.buildRoundReset()));
    }

    return allPreceding.build();
  }

  // Nondeterministic Assignments and Conditions

  private ImmutableList<CExportStatement> buildNondeterministicAssignments() {
    ImmutableList.Builder<CExportStatement> assignments = ImmutableList.builder();
    if (options.nondeterminismSource().isNextThreadNondeterministic()) {
      assignments.add(
          new CStatementWrapper(
              VerifierNondetFunctionType.buildNondetIntegerAssignment(
                  options, SeqIdExpressions.NEXT_THREAD)));
    }
    if (options.nondeterminismSource().isNumStatementsNondeterministic()) {
      assignments.add(
          new CStatementWrapper(
              VerifierNondetFunctionType.buildNondetIntegerAssignment(
                  options, SeqIdExpressions.ROUND_MAX)));
    }
    return assignments.build();
  }

  private CExportExpression buildNondeterministicIfCondition(MPORThread pThread)
      throws UnrecognizedCodeException {

    CExpression ifCondition =
        switch (options.nondeterminismSource()) {
          case NEXT_THREAD, NEXT_THREAD_AND_NUM_STATEMENTS ->
              utils
                  .binaryExpressionBuilder()
                  .buildBinaryExpression(
                      SeqIdExpressions.NEXT_THREAD,
                      SeqExpressionBuilder.buildIntegerLiteralExpression(pThread.id()),
                      BinaryOperator.EQUALS);
          case NUM_STATEMENTS ->
              utils
                  .binaryExpressionBuilder()
                  .buildBinaryExpression(
                      SeqIdExpressions.ROUND_MAX,
                      SeqIntegerLiteralExpressions.INT_0,
                      BinaryOperator.GREATER_THAN);
        };
    return new CExpressionWrapper(ifCondition);
  }

  // Multi Selection Statements

  /**
   * Creates the {@link CExportStatement} based on the specified {@link
   * MultiSelectionStatementEncoding}.
   */
  private static CExportStatement buildMultiSelectionStatementByEncoding(
      MultiSelectionStatementEncoding pEncoding,
      CLeftHandSide pExpression,
      ImmutableListMultimap<CExportExpression, CCompoundStatementElement> pStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build statements for control encoding " + pEncoding);
      case BINARY_SEARCH_TREE ->
          CMultiSelectionStatementBuilder.buildBinarySearchTree(
              ProgramCounterVariables.INIT_PC, pExpression, pStatements, pBinaryExpressionBuilder);
      case IF_ELSE_CHAIN -> CMultiSelectionStatementBuilder.buildIfElseChain(pStatements);
      case SWITCH_CASE -> new CSwitchStatement(pExpression, pStatements);
    };
  }

  /**
   * Creates the core i.e. the {@link MultiSelectionStatementEncoding} statement of a thread
   * simulation. The logic is common for all {@link NondeterminismSource}s.
   */
  private CExportStatement buildSingleThreadMultiSelectionStatement(MPORThread pThread)
      throws UnrecognizedCodeException {

    ImmutableList<SeqThreadStatementClause> withInjectedStatements =
        NondeterministicSimulationBuilder.tryInjectStatementsIntoClauses(
            options, clauses.get(pThread), utils.binaryExpressionBuilder());

    CLeftHandSide pcLeftHandSide = ghostElements.getPcVariables().getPcLeftHandSide(pThread.id());
    ImmutableListMultimap<CExportExpression, CCompoundStatementElement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            options, pcLeftHandSide, withInjectedStatements, utils.binaryExpressionBuilder());

    return buildMultiSelectionStatementByEncoding(
        options.controlEncodingStatement(),
        pcLeftHandSide,
        expressionClauseMap,
        utils.binaryExpressionBuilder());
  }
}
