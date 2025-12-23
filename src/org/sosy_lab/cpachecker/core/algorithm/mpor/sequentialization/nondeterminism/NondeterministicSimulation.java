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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
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
   * Builds the {@link String} code of the single simulation of {@code pActiveThread}. This is used
   * only when {@link MPOROptions#loopUnrolling()} is enabled, since it places the self-contained
   * thread simulations into a separate function for each thread.
   */
  abstract String buildSingleThreadSimulation(MPORThread pActiveThread)
      throws UnrecognizedCodeException;

  /**
   * Builds the {@link String} code of all thread simulations, including wrapper statements such as
   * {@code if} guards. This is used only when {@link MPOROptions#loopUnrolling()} is disabled,
   * since then all thread simulations are placed as one code block in the {@link main} function.
   */
  public abstract String buildAllThreadSimulations() throws UnrecognizedCodeException;

  /**
   * Builds list of statements that are placed directly before the simulation of {@code
   * pActiveThread}, e.g. assumptions. This can differ significantly based on {@link
   * NondeterminismSource}.
   */
  abstract ImmutableList<CStatement> buildPrecedingStatements(MPORThread pActiveThread)
      throws UnrecognizedCodeException;
}
