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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqPruner {

  // TODO binary expressions such as (A || B) are transformed into multiple edges in the cfa
  //  due to short circuit evaluation
  //  -> identify assume edges that map to the same input expression and merge into single case

  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pruneClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rPruned =
        ImmutableMap.builder();
    for (var entry : pClauses.entrySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = entry.getValue();
      if (isPrunable(clauses)) {
        MPORThread thread = entry.getKey();
        // if all case clauses are prunable then we want to include only the thread termination case
        //  e.g. goblint-regression/13-privatized_66-mine-W-init_true.i (t_fun exits immediately)
        if (allPrunable(clauses)) {
          // TODO we should check that there are not multiple thread exits when all are prunable
          SeqThreadStatementClause threadExit = getThreadExitClause(clauses);
          // ensure that the single thread exit case clause has label INIT_PC
          rPruned.put(
              thread,
              ImmutableList.of(
                  threadExit.labelNumber == Sequentialization.INIT_PC
                      ? threadExit
                      : threadExit.cloneWithBlock(
                          threadExit.block.cloneWithLabelNumber(Sequentialization.INIT_PC))));
        } else {
          rPruned.put(thread, pruneSingleThreadClauses(clauses));
        }
      }
    }
    // the initial pc are (often) not targeted here, we update them later to INIT_PC
    //  -> no validation of cases here
    return rPruned.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> pruneSingleThreadClauses(
      ImmutableList<SeqThreadStatementClause> pClauses) throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    // map the original pc to pc that are found after pruning
    ImmutableMap<Integer, Integer> pcUpdates = createPrunedPcUpdates(pClauses, labelClauseMap);
    // update each target pc so that it targets a non-blank case
    ImmutableList<SeqThreadStatementClause> rUpdatedTargetPc =
        updateTargetPcToNonPruned(pClauses, labelClauseMap, pcUpdates);
    Verify.verify(!isPrunable(rUpdatedTargetPc), "pruned case clauses are still prunable");
    return rUpdatedTargetPc;
  }

  /**
   * Maps pre prune {@code int pc} to their post prune counterparts. Not all target {@code pc} are
   * present as keys.
   */
  private static ImmutableMap<Integer, Integer> createPrunedPcUpdates(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap)
      throws UnrecognizedCodeException {

    Set<Integer> visitedPrePrunePc = new HashSet<>();
    ImmutableMap.Builder<Integer, Integer> rMap = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      if (!clause.onlyWritesPc()) {
        for (SeqThreadStatement statement : clause.block.getStatements()) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            Optional<Integer> postPrunePc = findTargetPc(clause, statement, pLabelClauseMap);
            if (postPrunePc.isPresent() && visitedPrePrunePc.add(targetPc)) {
              rMap.put(targetPc, postPrunePc.orElseThrow());
            }
          }
        }
      }
    }
    return rMap.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> updateTargetPcToNonPruned(
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, Integer> pPcUpdates) {

    ImmutableList.Builder<SeqThreadStatementClause> rUpdatedTargetPc = ImmutableList.builder();
    for (int i = 0; i < pClauses.size(); i++) {
      SeqThreadStatementClause clause = pClauses.get(i);
      if (isEmptyAtomicBlock(clause, pLabelClauseMap)) {
        // skip the empty atomic block
        i++;
        continue;
      }
      if (!clause.onlyWritesPc() && !isEmptyAtomicBlock(clause, pLabelClauseMap)) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : clause.block.getStatements()) {
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
        SeqThreadStatementBlock newBlock = clause.block.cloneWithStatements(newStatements.build());
        rUpdatedTargetPc.add(clause.cloneWithBlock(newBlock));
      }
    }
    return rUpdatedTargetPc.build();
  }

  private static Optional<Integer> findTargetPc(
      SeqThreadStatementClause pClause,
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap)
      throws UnrecognizedCodeException {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause nextClause = requireNonNull(pLabelClauseMap.get(targetPc));
        if (nextClause.onlyWritesPc() || isEmptyAtomicBlock(nextClause, pLabelClauseMap)) {
          SeqThreadStatementClause nonBlank =
              recursivelyFindNonBlankClause(Optional.of(pClause), nextClause, pLabelClauseMap);
          return Optional.of(extractTargetPc(nonBlank));
        } else {
          return Optional.of(extractTargetPc(nextClause));
        }
      }
    }
    return Optional.empty();
  }

  private static int extractTargetPc(SeqThreadStatementClause pNonBlank)
      throws UnrecognizedCodeException {
    SeqThreadStatement nonBlankSingleStatement = pNonBlank.block.getFirstStatement();

    if (nonBlankSingleStatement instanceof SeqBlankStatement blankStatement) {
      // the blank could have injected statements -> treat it as non-blank
      if (blankStatement.onlyWritesPc()) {
        Verify.verify(validPrunableClause(pNonBlank));
        int nonBlankTargetPc = nonBlankSingleStatement.getTargetPc().orElseThrow();
        // if the found non-blank is still blank, it must be an exit location
        Verify.verify(nonBlankTargetPc == Sequentialization.EXIT_PC);
        return nonBlankTargetPc;
      }
    }
    // otherwise return label pc of the found non-blank
    return pNonBlank.labelNumber;
  }

  private static SeqThreadStatementClause getThreadExitClause(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        if (statement.getTargetPc().orElseThrow() == Sequentialization.EXIT_PC) {
          return clause;
        }
      }
    }
    throw new AssertionError("no thread exit found in pClauses");
  }

  /**
   * Returns the first non-prunable {@link SeqThreadStatementClause} in the {@code case} path,
   * starting in pInitial.
   */
  public static SeqThreadStatementClause recursivelyFindNonBlankClause(
      final Optional<SeqThreadStatementClause> pInitial,
      SeqThreadStatementClause pCurrent,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap)
      throws UnrecognizedCodeException {

    // if pInitial is present, it should only write a pc
    checkArgument(
        pInitial.isEmpty() || !pInitial.orElseThrow().onlyWritesPc(),
        "pInitial must not be prunable");
    if (pCurrent.onlyWritesPc()) {
      SeqThreadStatement singleStatement = pCurrent.block.getFirstStatement();
      Verify.verify(validPrunableClause(pCurrent));
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause nextClause = requireNonNull(pLabelClauseMap.get(targetPc));
        return recursivelyFindNonBlankClause(pInitial, nextClause, pLabelClauseMap);
      }
    }
    if (isEmptyAtomicBlock(pCurrent, pLabelClauseMap)) {
      SeqThreadStatement singleStatement = pCurrent.block.getFirstStatement();
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      assert targetPc != Sequentialization.EXIT_PC : "atomic begin should not exit thread";
      SeqThreadStatementClause nextClause = requireNonNull(pLabelClauseMap.get(targetPc));
      SeqThreadStatement nextSingleStatement = nextClause.block.getFirstStatement();
      int nextTargetPc = nextSingleStatement.getTargetPc().orElseThrow();
      if (nextTargetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause nextNextClause = requireNonNull(pLabelClauseMap.get(nextTargetPc));
        return recursivelyFindNonBlankClause(pInitial, nextNextClause, pLabelClauseMap);
      }
    }
    return pCurrent;
  }

  /**
   * Returns {@code true} if {@code pClause} has exactly 1 {@link SeqBlankStatement} and a target
   * {@code pc} and throws a {@link IllegalArgumentException} otherwise.
   */
  private static boolean validPrunableClause(SeqThreadStatementClause pClause)
      throws UnrecognizedCodeException {

    checkArgument(
        pClause.block.getStatements().size() == 1,
        "prunable case clauses must contain exactly 1 statement: %s",
        pClause.toASTString());
    SeqThreadStatement statement = pClause.block.getFirstStatement();
    checkArgument(
        statement.onlyWritesPc(),
        "prunable statement must only write pc: %s",
        statement.toASTString());
    checkArgument(
        statement.getTargetPc().isPresent(), "prunable statement must contain a target pc");
    return true;
  }

  /**
   * Returns {@code true} if any {@link SeqThreadStatementClause} can be pruned, i.e. contains only
   * blank statements, and {@code false} otherwise.
   */
  private static boolean isPrunable(ImmutableList<SeqThreadStatementClause> pClauses) {
    for (SeqThreadStatementClause clause : pClauses) {
      if (clause.onlyWritesPc()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if all {@link SeqThreadStatementClause}s can be pruned, i.e. contains only
   * blank statements, and {@code false} otherwise.
   */
  private static boolean allPrunable(ImmutableList<SeqThreadStatementClause> pClauses) {
    for (SeqThreadStatementClause clause : pClauses) {
      if (!clause.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if {@code pClause} is an atomic begin and its direct successor is an atomic end,
   * i.e. there are no actual statements between the two.
   */
  private static boolean isEmptyAtomicBlock(
      SeqThreadStatementClause pClause,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    SeqThreadStatement singleStatement = pClause.block.getFirstStatement();
    if (singleStatement instanceof SeqAtomicBeginStatement) {
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        assert Math.abs(pClause.block.getLabel().labelNumber - targetPc) == 1
            : "absolute difference of empty atomic block labels must be 1";
        SeqThreadStatementClause target = requireNonNull(pLabelClauseMap.get(targetPc));
        return target.block.getFirstStatement() instanceof SeqAtomicEndStatement;
      }
    }
    return false;
  }
}
