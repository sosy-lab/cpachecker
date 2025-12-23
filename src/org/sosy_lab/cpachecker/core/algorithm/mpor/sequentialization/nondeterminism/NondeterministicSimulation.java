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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public abstract class NondeterministicSimulation {

  final MPOROptions options;

  final ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses;

  final GhostElements ghostElements;

  final SequentializationUtils utils;

  NondeterministicSimulation(
      MPOROptions pOptions,
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
  SeqMultiControlStatement buildSingleThreadMultiControlStatement(MPORThread pActiveThread)
      throws UnrecognizedCodeException {

    CIdExpression syncFlag = ghostElements.threadSyncFlags().getSyncFlag(pActiveThread);
    ImmutableList<SeqThreadStatementClause> withInjectedStatements =
        NondeterministicSimulationBuilder.injectStatementsIntoSingleThreadClauses(
            options, syncFlag, clauses.get(pActiveThread), utils.binaryExpressionBuilder());

    CLeftHandSide pcLeftHandSide =
        ghostElements.getPcVariables().getPcLeftHandSide(pActiveThread.id());
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            options, pcLeftHandSide, withInjectedStatements, utils.binaryExpressionBuilder());

    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        options.controlEncodingStatement(),
        pcLeftHandSide,
        buildPrecedingStatements(pActiveThread),
        expressionClauseMap,
        utils.binaryExpressionBuilder());
  }

  /**
   * Builds the {@link String} code of the single simulation of {@code pActiveThread}. This is used
   * only when {@link MPOROptions#loopUnrolling()} is enabled, since it places the self-contained
   * thread simulations into a separate function for each thread.
   */
  abstract String buildSingleThreadSimulation(MPORThread pActiveThread)
      throws UnrecognizedCodeException;

  /**
   * Builds the {@link String} code of all thread simulations, including wrapper statements such as
   * {@code if} guards. This is used only when {@link MPOROptions#loopUnrolling()} is disabled,
   * since then all thread simulations are placed as one code block in the {@code main()} function.
   */
  public abstract String buildAllThreadSimulations() throws UnrecognizedCodeException;

  /**
   * Builds list of statements that are placed directly before the simulation of a single {@code
   * pActiveThread}, e.g. assumptions or assignments, build via {@link
   * #buildSingleThreadSimulation(MPORThread)}.
   */
  abstract ImmutableList<CStatement> buildPrecedingStatements(MPORThread pActiveThread)
      throws UnrecognizedCodeException;
}
