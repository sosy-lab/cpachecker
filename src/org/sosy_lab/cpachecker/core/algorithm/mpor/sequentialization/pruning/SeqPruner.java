// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqPruner {

  /**
   * Prunes all {@link SeqCaseClause}s of {@link MPORThread}s so that no {@link SeqBlankStatement}s
   * are present in the pruned version and updates {@code pc} accordingly.
   *
   * <p>This method ensures that all {@code pc} are valid i.e. there is no {@code pc} assignment
   * that is not present in a threads simulation as a {@code case} label.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pruneCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses, LogManager pLogger)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> pruned = ImmutableMap.builder();
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
          pruned.put(
              thread,
              ImmutableList.of(
                  threadExit.label.value == SeqUtil.INIT_PC
                      ? threadExit
                      : threadExit.cloneWithLabel(new SeqCaseLabel(SeqUtil.INIT_PC))));
        } else {
          pruned.put(thread, pruneSingleThreadCaseClauses(caseClauses));
        }
      }
    }
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> rPruned = pruned.buildOrThrow();
    return SeqValidator.validateCaseClauses(rPruned, pLogger);
  }

  /**
   * Extracts {@link SeqCaseClause}s that are not {@link SeqBlankStatement}s from pCaseClauses and
   * updates the {@code pc} accordingly.
   *
   * <p>This method ensures that the returned, pruned {@link SeqCaseClause} cannot be pruned
   * further.
   */
  private static ImmutableList<SeqCaseClause> pruneSingleThreadCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqCaseClause> caseLabelValueMap =
        mapCaseLabelValueToCaseClauses(pCaseClauses);
    // map from case label pruned pcs to their new pcs after step 1 pruning
    Map<Integer, Integer> prunePcs = new HashMap<>();
    ImmutableList.Builder<SeqCaseClause> prune1 = ImmutableList.builder();
    // TODO add comment here why we need to track prunable case clauses?
    Set<SeqCaseClause> prunable = new HashSet<>();
    Set<Long> newIds = new HashSet<>();

    // step 1: recursively prune by executing chains of blank cases until a non-blank is found
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (prunable.add(caseClause)) {
        if (caseClause.isPrunable()) {
          int pc = caseClause.label.value;
          SeqCaseClause nonBlank =
              findNonBlankCaseClause(caseLabelValueMap, caseClause, prunable, caseClause);
          int nonBlankPc = nonBlank.label.value;
          if (!nonBlank.isPrunable()) {
            if (prunePcs.containsKey(nonBlankPc)) {
              // a nonBlank may be reachable through multiple blank paths
              // -> reference the first clone to prevent duplication of cases
              prunePcs.put(pc, prunePcs.get(nonBlankPc));
            } else {
              prune1.add(nonBlank.cloneWithLabel(caseClause.label));
              newIds.add(nonBlank.id);
              prunePcs.put(nonBlankPc, pc);
            }
          } else {
            // non-blank still prunable -> path leads to thread exit node
            assert nonBlank.block.statements.size() == 1;
            int targetPc = nonBlank.block.statements.get(0).getTargetPc().orElseThrow();
            assert targetPc == SeqUtil.EXIT_PC;
            prunePcs.put(nonBlankPc, targetPc);
            // pcs not equal -> thread exit reachable through multiple paths -> add both to pruned
            if (pc != nonBlankPc) {
              prunePcs.put(pc, targetPc);
            }
          }
        } else if (!newIds.contains(caseClause.id)) {
          prune1.add(caseClause);
          newIds.add(caseClause.id);
        }
      }
    }
    // step 2: update targetPcs if they point to a pruned pc
    ImmutableList.Builder<SeqCaseClause> prune2 = ImmutableList.builder();
    for (SeqCaseClause caseClause : prune1.build()) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStmts = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        Optional<Integer> targetPc = statement.getTargetPc();
        // if the statement targets a pruned pc, clone it with the new target pc
        if (targetPc.isPresent() && prunePcs.containsKey(targetPc.orElseThrow())) {
          int newTargetPc = prunePcs.get(targetPc.orElseThrow());
          SeqCaseBlockStatement clone = statement.cloneWithTargetPc(newTargetPc);
          assert clone.getClass().equals(statement.getClass())
              : "clone class must equal original statement class";
          newStmts.add(clone);
        } else {
          // otherwise, add unchanged statement
          newStmts.add(statement);
        }
      }
      prune2.add(
          caseClause.cloneWithBlock(new SeqCaseBlock(newStmts.build(), Terminator.CONTINUE)));
    }
    ImmutableList<SeqCaseClause> rPrune = prune2.build();
    Verify.verify(!isPrunable(rPrune));
    return rPrune;
  }

  /**
   * Returns the first {@link SeqCaseClause} in the {@code pc} chain that has no {@link
   * SeqBlankStatement}. If pInit reaches a threads termination through only blank statement,
   * returns pInit, i.e. the first blank statement.
   */
  private static SeqCaseClause findNonBlankCaseClause(
      final ImmutableMap<Integer, SeqCaseClause> pCaseLabelValueMap,
      final SeqCaseClause pInit,
      Set<SeqCaseClause> pPruned,
      SeqCaseClause pCurrent) {

    for (SeqCaseBlockStatement stmt : pCurrent.block.statements) {
      if (pCurrent.isPrunable()) {
        pPruned.add(pCurrent);
        SeqBlankStatement blank = (SeqBlankStatement) stmt;
        SeqCaseClause nextCaseClause = pCaseLabelValueMap.get(blank.getTargetPc().orElseThrow());
        if (nextCaseClause == null) {
          // this is only reachable if it is a threads exit (no successors)
          assert pCurrent.block.statements.size() == 1;
          int targetPc = pCurrent.block.statements.get(0).getTargetPc().orElseThrow();
          assert targetPc == SeqUtil.EXIT_PC;
          return pCurrent;
        }
        // do not visit exit nodes of the threads cfa
        if (!nextCaseClause.block.statements.isEmpty()) {
          return findNonBlankCaseClause(pCaseLabelValueMap, pInit, pPruned, nextCaseClause);
        }
      }
      // otherwise break recursion -> non-blank case found
      return pCurrent;
    }
    throw new IllegalArgumentException("pCurrent statements cannot be empty");
  }

  // Helpers =======================================================================================

  /**
   * A helper mapping {@link SeqCaseClause}s to their {@link SeqCaseLabel} values, which are always
   * {@code int} values in the sequentialization.
   */
  private static ImmutableMap<Integer, SeqCaseClause> mapCaseLabelValueToCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, SeqCaseClause> rOriginPcs = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      rOriginPcs.put(caseClause.label.value, caseClause);
    }
    return rOriginPcs.buildOrThrow();
  }

  private static SeqCaseClause getThreadExitCaseClause(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        if (statement.getTargetPc().orElseThrow() == SeqUtil.EXIT_PC) {
          return caseClause;
        }
      }
    }
    throw new AssertionError("no thread exit found in pCaseClauses");
  }

  /**
   * Returns {@code true} if any {@link SeqCaseClause} can be pruned, i.e. contains only blank
   * statements, and {@code false} otherwise.
   */
  private static boolean isPrunable(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (caseClause.isPrunable()) {
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
      if (!caseClause.isPrunable()) {
        return false;
      }
    }
    return true;
  }
}
