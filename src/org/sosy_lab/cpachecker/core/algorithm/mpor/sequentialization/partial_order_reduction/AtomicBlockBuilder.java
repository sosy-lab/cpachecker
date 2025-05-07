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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqSwitchCaseGotoLabelStatement;
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
      ImmutableMap<Integer, SeqSwitchCaseGotoLabelStatement> labelMap =
          SeqThreadStatementClauseUtil.mapLabelNumbersToLabels(clauses);
      rWithBlocks.put(thread, injectAtomicGotos(clauses, labelMap));
    }
    return rWithBlocks.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> injectAtomicGotos(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqSwitchCaseGotoLabelStatement> pLabelNumberMap) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithGotos = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.statements) {
        // TODO this means that if a statement contains an atomic_end in any branch, the other
        //  branches are not instrumented with goto, but have only the pc write -> fix
        // only replace target pc with target goto when in atomic block
        if (SeqThreadStatementUtil.targetsAtomicBlock(statement)) {
          newStatements.add(
              SeqThreadStatementClauseUtil.recursivelyReplaceTargetPcWithTargetGoto(
                  statement, pLabelNumberMap));
        } else {
          newStatements.add(statement);
        }
      }
      rWithGotos.add(clause.cloneWithBlock(new SeqThreadStatementBlock(newStatements.build())));
    }
    return rWithGotos.build();
  }
}
