// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqPruner {

  // TODO binary expressions such as (A || B) are transformed into multiple edges in the cfa
  //  due to short circuit evaluation
  //  -> identify assume edges that map to the same input expression and merge into single case

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pruneCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rPruned = ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      ImmutableList<SeqCaseClause> caseClauses = entry.getValue();
      if (isPrunable(caseClauses)) {
        MPORThread thread = entry.getKey();
        // if all case clauses are prunable then we want to include only the thread termination case
        //  e.g. goblint-regression/13-privatized_66-mine-W-init_true.i (t_fun exits immediately)
        if (allPrunable(caseClauses)) {
          // TODO we should check that there are not multiple thread exits when all are prunable
          SeqCaseClause threadExit = getThreadExitCaseClause(caseClauses);
          // ensure that the single thread exit case clause has label INIT_PC
          rPruned.put(
              thread,
              ImmutableList.of(
                  threadExit.label.value == Sequentialization.INIT_PC
                      ? threadExit
                      : threadExit.cloneWithLabel(new SeqCaseLabel(Sequentialization.INIT_PC))));
        } else {
          rPruned.put(thread, pruneSingleThreadCaseClauses(caseClauses));
        }
      }
    }
    // the initial pc are (often) not targeted here, we update them later to INIT_PC
    //  -> no validation of cases here
    return rPruned.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> pruneSingleThreadCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    // map the original pc to pc that are found after pruning
    ImmutableMap<Integer, Integer> pcUpdates = createPrunedPcUpdates(pCaseClauses);
    // update each target pc so that it targets a non-blank case
    ImmutableList<SeqCaseClause> rUpdatedTargetPc =
        updateTargetPcToNonPruned(pCaseClauses, pcUpdates);
    Verify.verify(!isPrunable(rUpdatedTargetPc), "pruned case clauses are still prunable");
    return rUpdatedTargetPc;
  }

  private static ImmutableList<SeqCaseClause> updateTargetPcToNonPruned(
      ImmutableList<SeqCaseClause> pCaseClauses, ImmutableMap<Integer, Integer> pPcUpdates) {

    ImmutableList.Builder<SeqCaseClause> rUpdatedTargetPc = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            if (pPcUpdates.containsKey(targetPc)) {
              // if pc was updated in prune, clone statement with new target pc
              newStatements.add(
                  statement.cloneWithTargetPc(Objects.requireNonNull(pPcUpdates.get(targetPc))));
              continue;
            }
          }
          // otherwise add unchanged statement
          newStatements.add(statement);
        }
        SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build(), Terminator.CONTINUE);
        rUpdatedTargetPc.add(caseClause.cloneWithBlock(newBlock));
      }
    }
    return rUpdatedTargetPc.build();
  }

  /**
   * Maps pre prune {@code int pc} to post prune {@link CExpression}. Not all target {@code pc} are
   * present as keys.
   */
  private static ImmutableMap<Integer, Integer> createPrunedPcUpdates(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    Set<Integer> visitedPrePrunePc = new HashSet<>();
    ImmutableMap.Builder<Integer, Integer> rMap = ImmutableMap.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            Optional<Integer> postPrunePc = findTargetPc(caseClause, statement, labelValueMap);
            if (postPrunePc.isPresent() && visitedPrePrunePc.add(targetPc)) {
              rMap.put(targetPc, postPrunePc.orElseThrow());
            }
          }
        }
      }
    }
    return rMap.buildOrThrow();
  }

  private static Optional<Integer> findTargetPc(
      SeqCaseClause pCaseClause,
      SeqCaseBlockStatement pStatement,
      ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqCaseClause nextCaseClause = requireNonNull(pLabelValueMap.get(targetPc));
        if (nextCaseClause.onlyWritesPc()) {
          SeqCaseClause nonBlank =
              findNonBlankCaseClause(pCaseClause, nextCaseClause, pLabelValueMap);
          return Optional.of(extractTargetPc(nonBlank));
        }
      }
    }
    return Optional.empty();
  }

  private static int extractTargetPc(SeqCaseClause pNonBlank) {
    SeqCaseBlockStatement nonBlankSingleStatement = pNonBlank.block.statements.get(0);

    if (nonBlankSingleStatement instanceof SeqBlankStatement blankStatement) {
      // the blank could have injected statements -> treat it as non-blank
      if (blankStatement.onlyWritesPc()) {
        Verify.verify(validPrunableCaseClause(pNonBlank));
        int nonBlankTargetPc = nonBlankSingleStatement.getTargetPc().orElseThrow();
        // if the found non-blank is still blank, it must be an exit location
        Verify.verify(nonBlankTargetPc == Sequentialization.EXIT_PC);
        return nonBlankTargetPc;
      }
    }

    // otherwise return label pc of the found non-blank
    return pNonBlank.label.value;
  }

  private static SeqCaseClause getThreadExitCaseClause(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        if (statement.getTargetPc().orElseThrow() == Sequentialization.EXIT_PC) {
          return caseClause;
        }
      }
    }
    throw new AssertionError("no thread exit found in pCaseClauses");
  }

  /**
   * Returns the first non-prunable {@link SeqCaseClause} in the {@code case} path, starting in
   * pInitial.
   */
  public static SeqCaseClause findNonBlankCaseClause(
      final SeqCaseClause pInitial,
      SeqCaseClause pCurrent,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    checkArgument(!pInitial.onlyWritesPc(), "pInitial must not be prunable");
    if (pCurrent.onlyWritesPc()) {
      SeqCaseBlockStatement singleStatement = pCurrent.block.statements.get(0);
      Verify.verify(validPrunableCaseClause(pCurrent));
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqCaseClause nextCaseClause = requireNonNull(pLabelValueMap.get(targetPc));
        return findNonBlankCaseClause(pInitial, nextCaseClause, pLabelValueMap);
      }
    }
    return pCurrent;
  }

  /**
   * Returns {@code true} if {@code pCaseClause} has exactly 1 {@link SeqBlankStatement} and a
   * target {@code pc} and throws a {@link IllegalArgumentException} otherwise.
   */
  private static boolean validPrunableCaseClause(SeqCaseClause pCaseClause) {
    checkArgument(
        pCaseClause.block.statements.size() == 1,
        "prunable case clauses must contain exactly 1 statement: %s",
        pCaseClause.toASTString());
    SeqCaseBlockStatement statement = pCaseClause.block.statements.get(0);
    checkArgument(
        statement.onlyWritesPc(),
        "prunable statement must only write pc: %s",
        statement.toASTString());
    checkArgument(
        statement.getTargetPc().isPresent(), "prunable statement must contain a target pc");
    return true;
  }

  /**
   * Returns {@code true} if any {@link SeqCaseClause} can be pruned, i.e. contains only blank
   * statements, and {@code false} otherwise.
   */
  private static boolean isPrunable(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (caseClause.onlyWritesPc()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if all {@link SeqCaseClause}s can be pruned, i.e. contains only blank
   * statements, and {@code false} otherwise.
   */
  private static boolean allPrunable(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }
}
