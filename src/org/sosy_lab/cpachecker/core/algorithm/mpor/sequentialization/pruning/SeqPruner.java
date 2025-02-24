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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqPruner {

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
          // TODO refactor and then delete .cloneWithLabel
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

  private static ImmutableList<SeqCaseClause> pruneSingleThreadCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseClause> pruned = ImmutableList.builder();
    // TODO the current reason why we dont use the list directly is that the unpruned cases have
    //  gaps in their label pcs -> try and refactor
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        mapCaseLabelValueToCaseClauses(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.isPrunable()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            if (targetPc != SeqUtil.EXIT_PC) {
              SeqCaseClause nextCaseClause = requireNonNull(labelValueMap.get(targetPc));
              if (nextCaseClause.isPrunable()) {
                // if next case clause is prunable ...
                SeqCaseClause nonBlank =
                    nonPrunableCaseClause(caseClause, nextCaseClause, labelValueMap);
                if (nonBlank.isPrunable()) {
                  Verify.verify(validPrunableCaseClause(nonBlank));
                  int nonBlankTargetPc =
                      nonBlank.block.statements.get(0).getTargetPc().orElseThrow();
                  Verify.verify(nonBlankTargetPc == SeqUtil.EXIT_PC);
                  // ... and non-blank targets the exit pc, use the target pc instead of label pc
                  SeqCaseBlockStatement clone = statement.cloneWithTargetPc(nonBlankTargetPc);
                  newStatements.add(clone);
                } else {
                  int nonBlankLabelPc = nonBlank.label.value;
                  // ... clone with first non-blank label pc as target
                  SeqCaseBlockStatement clone = statement.cloneWithTargetPc(nonBlankLabelPc);
                  newStatements.add(clone);
                }
                continue;
              }
            }
          }
          // ... otherwise add statement as is
          newStatements.add(statement);
        }
        SeqCaseBlock newBlock = caseClause.block.cloneWithStatements(newStatements.build());
        pruned.add(caseClause.cloneWithBlock(newBlock));
      }
    }
    ImmutableList<SeqCaseClause> rPruned = pruned.build();
    Verify.verify(!isPrunable(rPruned));
    return rPruned;
  }

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
   * Returns the first non-prunable {@link SeqCaseClause} in the {@code case} path, starting in
   * pInitial.
   */
  public static SeqCaseClause nonPrunableCaseClause(
      final SeqCaseClause pInitial,
      SeqCaseClause pCurrent,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    checkArgument(!pInitial.isPrunable(), "pInitial must not be prunable");
    if (pCurrent.isPrunable()) {
      Verify.verify(validPrunableCaseClause(pCurrent));
      int targetPc = pCurrent.block.statements.get(0).getTargetPc().orElseThrow();
      if (targetPc != SeqUtil.EXIT_PC) {
        SeqCaseClause nextCaseClause = requireNonNull(pLabelValueMap.get(targetPc));
        return nonPrunableCaseClause(pInitial, nextCaseClause, pLabelValueMap);
      }
    }
    return pCurrent;
  }

  /**
   * Returns {@code true} if {@code pCaseClause} has exactly 1 {@link SeqBlankStatement} and a
   * target {@code pc} and throws a {@link IllegalArgumentException} if not.
   */
  private static boolean validPrunableCaseClause(SeqCaseClause pCaseClause) {
    checkArgument(
        pCaseClause.block.statements.size() == 1,
        "prunable case clauses must contain exactly 1 statement");
    SeqCaseBlockStatement statement = pCaseClause.block.statements.get(0);
    checkArgument(
        statement instanceof SeqBlankStatement,
        "prunable case clauses must contain exactly 1 blank statement");
    checkArgument(
        statement.getTargetPc().isPresent(), "prunable case clauses must contain a target pc");
    return true;
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
