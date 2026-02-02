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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.StatementInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record PartialOrderReducer(
    MPOROptions options,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    Optional<BitVectorVariables> bitVectorVariables,
    Optional<MemoryModel> memoryModel,
    SequentializationUtils utils) {

  /**
   * Applies a Partial Order Reduction based on the settings in {@code pOptions}, or returns {@code
   * clauses} as is if disabled.
   */
  public ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reduce()
      throws UnrecognizedCodeException {

    if (options.linkReduction()) {
      StatementLinker statementLinker = new StatementLinker(options, memoryModel.orElseThrow());
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
          statementLinker.link(clauses);

      // first check shortcuts: are injections necessary?
      if (memoryModel.orElseThrow().getRelevantMemoryLocationAmount() == 0) {
        utils
            .logger()
            .log(
                Level.INFO,
                "bit vectors are enabled, but the program does not contain any global memory"
                    + " locations.");
        return linked; // no relevant memory locations -> no bit vectors needed

      } else if (!options.isAnyBitVectorReductionEnabled()) {
        return linked;

      } else {
        return tryInjectStatements();
      }
    }
    return clauses;
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
