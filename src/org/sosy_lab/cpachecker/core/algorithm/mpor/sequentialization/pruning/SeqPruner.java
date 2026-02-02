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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqGhostOnlyStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqPruner {

  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pruneClauses(
      MPOROptions pOptions, ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rPruned =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
      if (isPrunable(clauses)) {
        // if all case clauses are prunable then we want to include only the thread termination case
        //  e.g. goblint-regression/13-privatized_66-mine-W-init_true.i (t_fun exits immediately)
        if (allPrunable(clauses)) {
          SeqThreadStatementClause threadExit = getAnyThreadExitClause(clauses);
          SeqThreadStatementBlock firstBlock = threadExit.getFirstBlock();
          rPruned.putAll(
              thread,
              ImmutableList.of(
                  // ensure that the single thread exit case clause has label INIT_PC
                  threadExit.labelNumber == ProgramCounterVariables.INIT_PC
                      ? threadExit
                      : threadExit.withFirstBlock(
                          firstBlock.withLabelNumber(ProgramCounterVariables.INIT_PC))));
        } else {
          rPruned.putAll(thread, pruneSingleThreadClauses(clauses));
        }
      }
    }
    SeqValidator.tryValidateNoBlankClauses(pOptions, rPruned.build());
    return rPruned.build();
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
      if (!clause.isBlank()) {
        for (CSeqThreadStatement statement : clause.getFirstBlock().getStatements()) {
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
      if (!clause.isBlank() && !isEmptyAtomicBlock(clause, pLabelClauseMap)) {
        ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
        for (CSeqThreadStatement statement : clause.getFirstBlock().getStatements()) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            if (pPcUpdates.containsKey(targetPc)) {
              // if pc was updated in prune, clone statement with new target pc
              newStatements.add(
                  statement.withTargetPc(Objects.requireNonNull(pPcUpdates.get(targetPc))));
              continue;
            }
          }
          // otherwise add unchanged statement
          newStatements.add(statement);
        }
        SeqThreadStatementBlock firstBlock = clause.getFirstBlock();
        SeqThreadStatementBlock newBlock = firstBlock.withStatements(newStatements.build());
        rUpdatedTargetPc.add(clause.withFirstBlock(newBlock));
      }
    }
    return rUpdatedTargetPc.build();
  }

  private static Optional<Integer> findTargetPc(
      SeqThreadStatementClause pClause,
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap)
      throws UnrecognizedCodeException {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause nextClause = requireNonNull(pLabelClauseMap.get(targetPc));
        if (nextClause.isBlank() || isEmptyAtomicBlock(nextClause, pLabelClauseMap)) {
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

  private static int extractTargetPc(SeqThreadStatementClause pClause)
      throws UnrecognizedCodeException {

    checkArgument(pClause.getBlocks().size() == 1, "pClause can only have a single block");
    CSeqThreadStatement firstStatement = pClause.getFirstBlock().getFirstStatement();
    // the "non-blank" clause can still be blank, but only if it is an exit location
    if (firstStatement.isOnlyPcWrite()) {
      Verify.verify(validPrunableClause(pClause));
      int nonBlankTargetPc = firstStatement.getTargetPc().orElseThrow();
      Verify.verify(nonBlankTargetPc == ProgramCounterVariables.EXIT_PC);
      return nonBlankTargetPc;
    }
    // if the clause is not blank, return label pc of the found non-blank
    return pClause.labelNumber;
  }

  private static SeqThreadStatementClause getAnyThreadExitClause(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    for (SeqThreadStatementClause clause : pClauses) {
      for (CSeqThreadStatement statement : clause.getFirstBlock().getStatements()) {
        if (statement.getTargetPc().orElseThrow() == ProgramCounterVariables.EXIT_PC) {
          return clause;
        }
      }
    }
    throw new AssertionError("no thread exit found in clauses");
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
        pInitial.isEmpty() || !pInitial.orElseThrow().isBlank(), "pInitial must not be prunable");
    if (pCurrent.isBlank()) {
      CSeqThreadStatement singleStatement = pCurrent.getFirstBlock().getFirstStatement();
      Verify.verify(validPrunableClause(pCurrent));
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause nextClause = requireNonNull(pLabelClauseMap.get(targetPc));
        return recursivelyFindNonBlankClause(pInitial, nextClause, pLabelClauseMap);
      }
    }
    if (isEmptyAtomicBlock(pCurrent, pLabelClauseMap)) {
      CSeqThreadStatement singleStatement = pCurrent.getFirstBlock().getFirstStatement();
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      assert targetPc != ProgramCounterVariables.EXIT_PC : "atomic begin should not exit thread";
      SeqThreadStatementClause nextClause = requireNonNull(pLabelClauseMap.get(targetPc));
      CSeqThreadStatement nextSingleStatement = nextClause.getFirstBlock().getFirstStatement();
      int nextTargetPc = nextSingleStatement.getTargetPc().orElseThrow();
      if (nextTargetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause nextNextClause = requireNonNull(pLabelClauseMap.get(nextTargetPc));
        return recursivelyFindNonBlankClause(pInitial, nextNextClause, pLabelClauseMap);
      }
    }
    return pCurrent;
  }

  /**
   * Returns {@code true} if {@code pClause} has exactly 1 {@link SeqGhostOnlyStatement} and a
   * target {@code pc} and throws a {@link IllegalArgumentException} otherwise.
   */
  private static boolean validPrunableClause(SeqThreadStatementClause pClause)
      throws UnrecognizedCodeException {

    checkArgument(
        pClause.getFirstBlock().getStatements().size() == 1,
        "prunable case clauses must contain exactly 1 statement: %s",
        pClause.toASTString());
    CSeqThreadStatement statement = pClause.getFirstBlock().getFirstStatement();
    checkArgument(
        statement.isOnlyPcWrite(), "prunable statement must be blank: %s", statement.toASTString());
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
      if (clause.isBlank()) {
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
      if (!clause.isBlank()) {
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

    CSeqThreadStatement singleStatement = pClause.getFirstBlock().getFirstStatement();
    if (singleStatement instanceof SeqAtomicBeginStatement) {
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        assert Math.abs(pClause.getFirstBlock().getLabel().getLabelNumber() - targetPc) == 1
            : "absolute difference of empty atomic block labels must be 1";
        SeqThreadStatementClause target = requireNonNull(pLabelClauseMap.get(targetPc));
        return target.getFirstBlock().getFirstStatement() instanceof SeqAtomicEndStatement;
      }
    }
    return false;
  }
}
