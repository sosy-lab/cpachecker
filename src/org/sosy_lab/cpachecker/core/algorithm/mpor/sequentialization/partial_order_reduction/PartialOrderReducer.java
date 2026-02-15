// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.StatementInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record PartialOrderReducer(
    MPOROptions options,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    Optional<SeqBitVectorVariables> bitVectorVariables,
    Optional<MemoryModel> memoryModel,
    SequentializationUtils utils) {

  /**
   * Applies a Partial Order Reduction based on the settings in {@code pOptions}, or returns {@code
   * clauses} as is if disabled.
   */
  public ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reduceClauses()
      throws UnrecognizedCodeException {

    // if linkReduction is disabled, then all other reduction options are disabled too
    if (!options().linkReduction()) {
      return clauses;
    }
    // if there are no relevant memory locations, then no injections are necessary
    if (memoryModel.orElseThrow().getRelevantMemoryLocationAmount() == 0) {
      utils
          .logger()
          .log(
              Level.INFO,
              "A partial order reduction option is enabled, but the input program does not contain"
                  + " any global memory locations.");
      return clauses;
    }
    // at least one reduction option must be enabled to inject any statement
    if (!options.isAnyBitVectorReductionEnabled()) {
      return clauses;
    }
    return tryInjectStatements();
  }

  private ImmutableListMultimap<MPORThread, SeqThreadStatementClause> tryInjectStatements()
      throws UnrecognizedCodeException {

    // otherwise inject into statements
    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rInjected =
        ImmutableListMultimap.builder();
    for (MPORThread activeThread : clauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> activeClauses = clauses.get(activeThread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(activeClauses);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(activeClauses);

      StatementInjector statementInjector =
          new StatementInjector(
              options,
              activeThread,
              MPORUtil.withoutElement(clauses.keySet(), activeThread),
              activeClauses,
              labelClauseMap,
              labelBlockMap,
              bitVectorVariables.orElseThrow(),
              memoryModel.orElseThrow(),
              utils);
      rInjected.putAll(activeThread, statementInjector.injectStatementsIntoClauses());
    }
    return rInjected.build();
  }
}
