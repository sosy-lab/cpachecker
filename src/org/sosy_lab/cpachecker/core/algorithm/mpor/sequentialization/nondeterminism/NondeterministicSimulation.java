// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIfStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.ReduceLastThreadOrderInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

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
public abstract class NondeterministicSimulation {

  final MPOROptions options;

  final Optional<MemoryModel> memoryModel;

  final ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses;

  final GhostElements ghostElements;

  final SequentializationUtils utils;

  NondeterministicSimulation(
      MPOROptions pOptions,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    // ensure that only the specified nondeterministic simulation is created
    switch (pOptions.nondeterminismSource()) {
      case NEXT_THREAD -> checkArgument(this instanceof NextThreadNondeterministicSimulation);
      case NEXT_THREAD_AND_NUM_STATEMENTS ->
          checkArgument(this instanceof NextThreadAndNumStatementsNondeterministicSimulation);
      case NUM_STATEMENTS -> checkArgument(this instanceof NumStatementsNondeterministicSimulation);
    }
    options = pOptions;
    memoryModel = pMemoryModel;
    ghostElements = pGhostElements;
    clauses = pClauses;
    utils = pUtils;
  }

  /**
   * Creates the core i.e. the {@link SeqMultiControlStatement} of a thread simulation used for
   * {@link NondeterministicSimulation#buildSingleThreadSimulation(MPORThread)}. The logic is common
   * for all {@link NondeterminismSource}s, so this is not tied to the separate implementations of
   * {@link NondeterministicSimulation}.
   */
  SeqMultiControlStatement buildSingleThreadMultiControlStatement(MPORThread pThread)
      throws UnrecognizedCodeException {

    CIdExpression syncFlag = ghostElements.threadSyncFlags().getSyncFlag(pThread);
    ImmutableList<SeqThreadStatementClause> withInjectedStatements =
        NondeterministicSimulationBuilder.injectStatementsIntoSingleThreadClauses(
            options, syncFlag, clauses.get(pThread), utils.binaryExpressionBuilder());

    CLeftHandSide pcLeftHandSide = ghostElements.getPcVariables().getPcLeftHandSide(pThread.id());
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            options, pcLeftHandSide, withInjectedStatements, utils.binaryExpressionBuilder());

    ImmutableList<String> precedingStatements =
        ImmutableList.<String>builder()
            .addAll(buildPrecedingReductionStatements(pThread))
            .addAll(buildPrecedingStatements(pThread))
            .build();

    return SeqMultiControlStatement.buildMultiControlStatementByEncoding(
        options.controlEncodingStatement(),
        pcLeftHandSide,
        precedingStatements,
        expressionClauseMap,
        utils.binaryExpressionBuilder());
  }

  /**
   * Builds the core reduction instrumentation of {@link MPOROptions#reduceLastThreadOrder()} that
   * precedes all thread simulations, if enabled.
   */
  private ImmutableList<String> buildPrecedingReductionStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rStatements = ImmutableList.builder();

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
                    memoryModel.orElseThrow(),
                    utils)
                .buildLastThreadOrderStatement(pThread);
        rStatements.add(lastThreadOrderStatement.toASTString());
      }
    }

    return rStatements.build();
  }

  /**
   * Builds the {@link String} code of a single simulation for the given {@code pThread}, including
   * the {@link SeqMultiControlStatement}.
   *
   * <p>The resulting {@link String} must make it possible for the simulation to be placed in a
   * separate function that can be called without any additional wrappers or preceding statements.
   * This is needed when {@link MPOROptions#loopUnrolling()} is enabled.
   *
   * <p>E.g., the code must check that {@code pThread} is currently active (e.g. {@code pc0 != 0}
   * for thread 0) and that it is non-deterministically chosen for simulation (e.g. that {@code
   * next_thread == 0} for {@link NondeterminismSource#NEXT_THREAD} or that {@code round_max > 0}
   * for {@link NondeterminismSource#NUM_STATEMENTS}). These checks can then be placed in {@code if}
   * statements or calls to {@code assume}. The latter should always be used if the analysis can
   * soundly prune the exploration of a thread simulation without underapproximating the state
   * space.
   */
  abstract String buildSingleThreadSimulation(MPORThread pThread) throws UnrecognizedCodeException;

  /**
   * Builds the {@link String} code of all thread simulations, including wrapper statements such as
   * {@code if} guards. This is used only when {@link MPOROptions#loopUnrolling()} is disabled,
   * since then all thread simulations are placed as one code block in the {@code main()} function.
   */
  public abstract String buildAllThreadSimulations() throws UnrecognizedCodeException;

  /**
   * Builds the list of statements, e.g. assumptions or assignments, that are placed directly before
   * the {@link SeqMultiControlStatement} of a single {@code pThread}.
   *
   * <p>Given that we are working with {@link CStatement}, the preceding statements can only contain
   * e.g. calls to {@code assume} or {@link CAssignment}s, not {@code if} guards that must be
   * handled by {@link NondeterministicSimulation#buildSingleThreadSimulation(MPORThread)}.
   *
   * <p>Nonetheless, everything that can be expressed using {@link CStatement}s must be included
   * here to combine the common functionality for use in {@link
   * NondeterministicSimulation#buildSingleThreadSimulation(MPORThread)}.
   */
  abstract ImmutableList<String> buildPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException;
}
