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
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

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
      ImmutableList<SeqThreadStatementClause> clauses = entry.getValue();
      ImmutableMap<Integer, SeqThreadStatementClause> labelMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      rWithBlocks.put(thread, injectAtomicGotos(clauses, labelMap));
    }
    return rWithBlocks.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> injectAtomicGotos(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelMap) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithGotos = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.statements) {
        newStatements.add(recursivelyReplaceTargetPcWithTargetGoto(statement, pLabelMap));
      }
      rWithGotos.add(clause.cloneWithBlock(new SeqThreadStatementBlock(newStatements.build())));
    }
    return rWithGotos.build();
  }

  private static SeqThreadStatement recursivelyReplaceTargetPcWithTargetGoto(
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelMap) {

    // if there are concatenated statements, replace target pc there too
    if (pCurrentStatement.isConcatenable()) {
      ImmutableList<SeqThreadStatement> concatenatedStatements =
          pCurrentStatement.getConcatenatedStatements();
      if (!concatenatedStatements.isEmpty()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement concatenatedStatement : concatenatedStatements) {
          if (concatenatedStatement instanceof SeqAtomicEndStatement) {
            // atomic_end -> stop replacing, need pc update and context switch
            newStatements.add(concatenatedStatement);
          } else {
            newStatements.add(
                recursivelyReplaceTargetPcWithTargetGoto(concatenatedStatement, pLabelMap));
          }
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }

    if (pCurrentStatement.getTargetPc().isPresent()) {
      // int target is present and there are no concatenated statements -> clone with targetIndex
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC && pLabelMap.containsKey(targetPc)) {
        SeqThreadStatementClause targetClause = Objects.requireNonNull(pLabelMap.get(targetPc));
        SeqThreadStatement firstStatement = targetClause.block.getFirstStatement();
        // only add goto when the target starts in an atomic block
        if (SeqThreadStatementUtil.startsInAtomicBlock(firstStatement)) {
          return pCurrentStatement.cloneWithTargetGoto(targetClause.gotoLabel.getLabelName());
        }
      }
    }
    // no int target pc -> no replacement
    return pCurrentStatement;
  }
}
