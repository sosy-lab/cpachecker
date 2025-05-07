// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

@SuppressWarnings("unused") // TODO remove later
public class AtomicBlockBuilder {

  /**
   * Builds atomic blocks for {@code pCaseClauses} by adding {@code goto} statements when
   * encountering an atomic_begin, until an atomic_end is encountered.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> build(
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rWithBlocks =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      // TODO inject direct gotos when in atomic block
      rWithBlocks.put(thread, entry.getValue());
    }
    return rWithBlocks.buildOrThrow();
  }

  // Atomic Concatenation ==========================================================================

  // TODO the concatenation of atomic blocks can result in enormous file sizes when using binary
  //  logical expressions such as || due to the branching in the CFA (see e.g. pthread-wmm/mix*)
  //  however, it reduces the number of statements that have to be searched enormously too, which
  //  may be beneficial for CBMC/ESBMC. but the parsing may fail, due to the huge file sizes.
  //  so, test this in practice

  private static ImmutableList<SeqThreadStatementClause> concatenateAtomicBlocks(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> newCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    Set<Integer> concatenated = new HashSet<>();
    Set<Integer> duplicated = new HashSet<>();

    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      // prevent start in already concatenated clauses, otherwise they are duplicated
      if (concatenated.add(caseClause.id)) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : caseClause.block.statements) {
          if (statement instanceof SeqAtomicBeginStatement) {
            // starting new atomic block with atomic begin -> concat until atomic end
            newStatements.add(
                recursivelyConcatenateStatements(
                    statement, concatenated, duplicated, labelValueMap));
          } else {
            // otherwise add statement as is
            newStatements.add(statement);
          }
        }
        SeqThreadStatementBlock newBlock = new SeqThreadStatementBlock(newStatements.build());
        SeqThreadStatementClause clone = caseClause.cloneWithBlock(newBlock);
        newCaseClauses.add(clone);
      }
    }
    ImmutableList<SeqThreadStatementClause> rNewCaseClauses = newCaseClauses.build();
    // we filter out case clauses that were visited twice during concatenation
    return rNewCaseClauses.stream()
        .filter(
            caseClause ->
                concatenated.contains(caseClause.id) && !duplicated.contains(caseClause.id))
        .collect(ImmutableList.toImmutableList());
  }

  /** Recursively concatenates the target statements of {@code pCurrentStatement}, if applicable. */
  private static SeqThreadStatement recursivelyConcatenateStatements(
      SeqThreadStatement pCurrentStatement,
      Set<Integer> pConcatenated,
      Set<Integer> pDuplicated,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelValueMap) {

    // when encountering atomic end, stop concatenation of atomic block
    if (!(pCurrentStatement instanceof SeqAtomicEndStatement)) {
      if (SeqThreadStatementClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
        int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
        SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
        // if the target id was seen before, add it to duplicate, except loop heads
        if (!pConcatenated.add(newTarget.id) && !newTarget.isLoopStart) {
          pDuplicated.add(newTarget.id);
        }
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : newTarget.block.statements) {
          newStatements.add(
              recursivelyConcatenateStatements(
                  statement, pConcatenated, pDuplicated, pLabelValueMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }
    return pCurrentStatement;
  }
}
